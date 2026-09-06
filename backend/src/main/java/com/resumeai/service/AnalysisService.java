package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalysisResponse;
import com.resumeai.dto.IssueDto;
import com.resumeai.entity.ResumeAnalysis;
import com.resumeai.entity.ResumeDocument;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ResumeAnalysisRepository;
import com.resumeai.repository.UserRepository;
import com.resumeai.service.analysis.Issue;
import com.resumeai.service.analysis.ScoringEngine;
import com.resumeai.service.billing.UsageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AnalysisService {

    private final ResumeUploadService resumeUploadService;
    private final ScoringEngine scoringEngine;
    private final ResumeAnalysisRepository analysisRepository;
    private final DocumentProcessingLockRegistry lockRegistry;
    private final UsageService usageService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisService(ResumeUploadService resumeUploadService,
                            ScoringEngine scoringEngine,
                            ResumeAnalysisRepository analysisRepository,
                            DocumentProcessingLockRegistry lockRegistry,
                            UsageService usageService,
                            UserRepository userRepository,
                            AnalyticsService analyticsService) {
        this.resumeUploadService = resumeUploadService;
        this.scoringEngine = scoringEngine;
        this.analysisRepository = analysisRepository;
        this.lockRegistry = lockRegistry;
        this.usageService = usageService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public AnalysisResponse analyze(UUID userId, UUID documentId) {
        ReentrantLock lock = lockRegistry.lockFor(documentId);
        if (!lock.tryLock()) {
            throw new ApiException(HttpStatus.CONFLICT, "ANALYSIS_IN_PROGRESS",
                    "An analysis for this resume is already running. Please wait for it to finish.");
        }

        try {
            ResumeDocument document = resumeUploadService.getOwnedDocument(documentId, userId);

            if (document.getStatus() != ResumeDocument.Status.PARSED
                    && document.getStatus() != ResumeDocument.Status.ANALYZED) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NOT_READY",
                        "This resume has not been successfully parsed yet and cannot be analyzed.");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Account not found"));
            usageService.checkAndIncrement(userId, user.getPlan(), UsageService.UsageType.ANALYSIS);

            String text = document.getExtractedText();
            int parsingConfidence = document.getParsingConfidence() == null ? 0 : document.getParsingConfidence();
            boolean likelyImageOnly = parsingConfidence < 40;
            List<String> parsingIssues = resumeUploadService.readParsingIssues(document);

            ScoringEngine.ScoreBreakdown breakdown = scoringEngine.score(text, parsingConfidence, parsingIssues, likelyImageOnly);

            String issuesJson = writeJson(breakdown.issues());
            String sectionsJson = writeJson(breakdown.detectedSections());

            ResumeAnalysis analysis = ResumeAnalysis.builder()
                    .documentId(documentId)
                    .resumeId(document.getResumeId())
                    .userId(userId)
                    .overallScore(breakdown.overallScore())
                    .atsParsingScore(breakdown.atsParsingScore())
                    .keywordAlignmentScore(breakdown.keywordAlignmentScore())
                    .structureScore(breakdown.structureScore())
                    .contentQualityScore(breakdown.contentQualityScore())
                    .experienceRelevanceScore(breakdown.experienceRelevanceScore())
                    .impactScore(breakdown.impactScore())
                    .formattingScore(breakdown.formattingScore())
                    .issuesJson(issuesJson)
                    .detectedSectionsJson(sectionsJson)
                    .build();

            analysisRepository.save(analysis);

            document.setStatus(ResumeDocument.Status.ANALYZED);

            analyticsService.record(userId, "resume_analyzed");

            return toResponse(analysis, breakdown);
        } finally {
            lock.unlock();
            lockRegistry.release(documentId, lock);
        }
    }

    public AnalysisResponse getLatestForDocument(UUID userId, UUID documentId) {
        resumeUploadService.getOwnedDocument(documentId, userId);
        ResumeAnalysis analysis = analysisRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis"));
        return toResponseFromStored(analysis);
    }

    private AnalysisResponse toResponse(ResumeAnalysis analysis, ScoringEngine.ScoreBreakdown breakdown) {
        List<IssueDto> issueDtos = breakdown.issues().stream()
                .map(i -> new IssueDto(i.category(), i.severity().name(), i.title(), i.explanation(), i.recommendation()))
                .toList();

        return new AnalysisResponse(
                analysis.getId(), analysis.getDocumentId(), analysis.getOverallScore(),
                new AnalysisResponse.CategoryScores(
                        breakdown.atsParsingScore(), breakdown.keywordAlignmentScore(), breakdown.structureScore(),
                        breakdown.contentQualityScore(), breakdown.experienceRelevanceScore(),
                        breakdown.impactScore(), breakdown.formattingScore()
                ),
                issueDtos,
                breakdown.detectedSections(),
                "This score is an estimate based on parsing reliability, structure, keyword alignment, and content quality. It does not guarantee ATS selection or an interview.",
                analysis.getCreatedAt()
        );
    }

    private AnalysisResponse toResponseFromStored(ResumeAnalysis analysis) {
        List<IssueDto> issues = readJsonList(analysis.getIssuesJson(), Issue.class).stream()
                .map(i -> new IssueDto(i.category(), i.severity().name(), i.title(), i.explanation(), i.recommendation()))
                .toList();
        List<String> sections = readJsonList(analysis.getDetectedSectionsJson(), String.class);

        return new AnalysisResponse(
                analysis.getId(), analysis.getDocumentId(), analysis.getOverallScore(),
                new AnalysisResponse.CategoryScores(
                        analysis.getAtsParsingScore(), analysis.getKeywordAlignmentScore(), analysis.getStructureScore(),
                        analysis.getContentQualityScore(), analysis.getExperienceRelevanceScore(),
                        analysis.getImpactScore(), analysis.getFormattingScore()
                ),
                issues, sections,
                "This score is an estimate based on parsing reliability, structure, keyword alignment, and content quality. It does not guarantee ATS selection or an interview.",
                analysis.getCreatedAt()
        );
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize analysis result", e);
        }
    }

    private <T> List<T> readJsonList(String json, Class<T> type) {
        try {
            if (json == null) return List.of();
            var factory = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
            return objectMapper.readValue(json, factory);
        } catch (Exception e) {
            return List.of();
        }
    }
}

