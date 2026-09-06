package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.ResumeUploadResponse;
import com.resumeai.entity.Resume;
import com.resumeai.entity.ResumeDocument;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ResumeDocumentRepository;
import com.resumeai.repository.ResumeRepository;
import com.resumeai.service.parsing.ResumeParsingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ResumeUploadService {

    private final FileSecurityService fileSecurityService;
    private final ResumeParsingService resumeParsingService;
    private final StorageService storageService;
    private final ResumeRepository resumeRepository;
    private final ResumeDocumentRepository resumeDocumentRepository;
    private final int parsingConfidenceWarnThreshold;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeUploadService(FileSecurityService fileSecurityService,
                                ResumeParsingService resumeParsingService,
                                StorageService storageService,
                                ResumeRepository resumeRepository,
                                ResumeDocumentRepository resumeDocumentRepository,
                                @Value("${app.scoring.parsing-confidence-warn-threshold}") int parsingConfidenceWarnThreshold,
                                AnalyticsService analyticsService) {
        this.fileSecurityService = fileSecurityService;
        this.resumeParsingService = resumeParsingService;
        this.storageService = storageService;
        this.resumeRepository = resumeRepository;
        this.resumeDocumentRepository = resumeDocumentRepository;
        this.parsingConfidenceWarnThreshold = parsingConfidenceWarnThreshold;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public ResumeUploadResponse upload(UUID userId, String label, MultipartFile file) {
        FileSecurityService.ValidatedFile validated = fileSecurityService.validate(file);

        var existing = resumeDocumentRepository.findByUserIdAndContentHash(userId, validated.contentHashSha256());
        if (existing.isPresent() && existing.get().getDeletedAt() == null) {
            ResumeDocument doc = existing.get();
            boolean warn = doc.getParsingConfidence() != null && doc.getParsingConfidence() < parsingConfidenceWarnThreshold;
            return new ResumeUploadResponse(
                    doc.getResumeId(), doc.getId(), "DUPLICATE_OF_EXISTING",
                    doc.getParsingConfidence() == null ? 0 : doc.getParsingConfidence(), warn);
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .label(label == null || label.isBlank() ? validated.sanitizedDisplayName() : label.trim())
                .build();
        resumeRepository.save(resume);

        ResumeDocument document = ResumeDocument.builder()
                .resumeId(resume.getId())
                .userId(userId)
                .originalFilename(validated.sanitizedDisplayName())
                .storageKey("pending")
                .detectedMimeType(validated.detectedMimeType())
                .declaredExtension(validated.extension())
                .fileSizeBytes(validated.bytes().length)
                .contentHash(validated.contentHashSha256())
                .status(ResumeDocument.Status.PARSING)
                .build();
        resumeDocumentRepository.save(document);

        String storageKey = storageService.store(userId, document.getId(), validated.bytes());
        document.setStorageKey(storageKey);

        try {
            ResumeParsingService.ParsedResume parsed = resumeParsingService.parse(validated.bytes(), validated.extension());
            document.setExtractedText(parsed.normalizedText());
            document.setParsingConfidence(parsed.parsingConfidence());
            document.setParsingIssuesJson(writeJson(parsed.parsingIssues()));
            document.setStatus(ResumeDocument.Status.PARSED);
        } catch (RuntimeException e) {
            document.setStatus(ResumeDocument.Status.PARSE_FAILED);
            resumeDocumentRepository.save(document);
            throw e;
        }

        resumeDocumentRepository.save(document);

        analyticsService.record(userId, "resume_uploaded");

        boolean warn = document.getParsingConfidence() != null
                && document.getParsingConfidence() < parsingConfidenceWarnThreshold;

        return new ResumeUploadResponse(
                resume.getId(), document.getId(), document.getStatus().name(),
                document.getParsingConfidence() == null ? 0 : document.getParsingConfidence(), warn);
    }

    public ResumeDocument getOwnedDocument(UUID documentId, UUID userId) {
        ResumeDocument document = resumeDocumentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume document"));
        if (document.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Resume document");
        }
        Resume resume = resumeRepository.findById(document.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume document"));
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Resume document");
        }
        return document;
    }

    public List<String> readParsingIssues(ResumeDocument document) {
        try {
            if (document.getParsingIssuesJson() == null) return List.of();
            var type = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return objectMapper.readValue(document.getParsingIssuesJson(), type);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize parsing issues", e);
        }
    }
}

