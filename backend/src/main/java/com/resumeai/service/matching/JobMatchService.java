package com.resumeai.service.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.JobDescriptionRequest;
import com.resumeai.dto.JobDescriptionResponse;
import com.resumeai.dto.JobMatchResponse;
import com.resumeai.entity.JobDescription;
import com.resumeai.entity.JobMatch;
import com.resumeai.entity.ResumeDocument;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.JobDescriptionRepository;
import com.resumeai.repository.JobMatchRepository;
import com.resumeai.repository.UserRepository;
import com.resumeai.service.ResumeUploadService;
import com.resumeai.service.billing.UsageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.resumeai.service.AnalyticsService;   

import java.util.List;
import java.util.UUID;

@Service
public class JobMatchService {

    private final JobDescriptionParser parser;
    private final JobMatchEngine matchEngine;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobMatchRepository jobMatchRepository;
    private final ResumeUploadService resumeUploadService;
    private final UsageService usageService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobMatchService(JobDescriptionParser parser, JobMatchEngine matchEngine,
                            JobDescriptionRepository jobDescriptionRepository,
                            JobMatchRepository jobMatchRepository,
                            ResumeUploadService resumeUploadService,
                            UsageService usageService,
                            UserRepository userRepository,
                            AnalyticsService analyticsService) {
        this.parser = parser;
        this.matchEngine = matchEngine;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.resumeUploadService = resumeUploadService;
        this.usageService = usageService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public JobDescriptionResponse submitJobDescription(UUID userId, JobDescriptionRequest request) {
        JobDescriptionParser.ParsedJd parsed = parser.parse(request.rawText());

        JobDescription entity = JobDescription.builder()
                .userId(userId)
                .rawText(request.rawText())
                .jobTitle(parsed.jobTitle())
                .seniority(parsed.seniority())
                .minYearsExperience(parsed.minYearsExperience())
                .requiredSkillsJson(writeJson(parsed.requiredSkills()))
                .preferredSkillsJson(writeJson(parsed.preferredSkills()))
                .build();
        jobDescriptionRepository.save(entity);

        return new JobDescriptionResponse(entity.getId(), entity.getJobTitle(), entity.getSeniority(),
                entity.getMinYearsExperience(), parsed.requiredSkills(), parsed.preferredSkills());
    }

    @Transactional
    public JobMatchResponse match(UUID userId, UUID documentId, UUID jobDescriptionId) {
        ResumeDocument document = resumeUploadService.getOwnedDocument(documentId, userId);
        JobDescription jd = jobDescriptionRepository.findByIdAndUserId(jobDescriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job description"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Account not found"));
        usageService.checkAndIncrement(userId, user.getPlan(), UsageService.UsageType.JD_MATCH);

        List<String> requiredSkills = readJson(jd.getRequiredSkillsJson());
        List<String> preferredSkills = readJson(jd.getPreferredSkillsJson());

        Integer estimatedYears = matchEngine.estimateYearsOfExperience(document.getExtractedText());
        JobMatchEngine.MatchOutcome outcome = matchEngine.match(
                document.getExtractedText(), requiredSkills, preferredSkills,
                jd.getMinYearsExperience(), estimatedYears);

        List<String> matchedSkills = new java.util.ArrayList<>(outcome.required().matched());
        matchedSkills.addAll(outcome.preferred().matched());

        JobMatch entity = JobMatch.builder()
                .documentId(documentId)
                .jobDescriptionId(jobDescriptionId)
                .userId(userId)
                .matchScore(outcome.matchScore())
                .requiredMatched(outcome.required().matchedCount())
                .requiredTotal(outcome.required().totalCount())
                .preferredMatched(outcome.preferred().matchedCount())
                .preferredTotal(outcome.preferred().totalCount())
                .experienceMatchLabel(outcome.experienceMatchLabel())
                .matchedSkillsJson(writeJson(matchedSkills))
                .missingRequiredSkillsJson(writeJson(outcome.required().missing()))
                .missingPreferredSkillsJson(writeJson(outcome.preferred().missing()))
                .build();
        jobMatchRepository.save(entity);

        analyticsService.record(userId, "job_match_run");

        return toResponse(entity);
    }

    private JobMatchResponse toResponse(JobMatch entity) {
        return new JobMatchResponse(
                entity.getId(), entity.getDocumentId(), entity.getJobDescriptionId(), entity.getMatchScore(),
                new JobMatchResponse.SkillBreakdown(
                        entity.getRequiredMatched(), entity.getRequiredTotal(),
                        readJson(entity.getMatchedSkillsJson()), readJson(entity.getMissingRequiredSkillsJson())),
                new JobMatchResponse.SkillBreakdown(
                        entity.getPreferredMatched(), entity.getPreferredTotal(),
                        readJson(entity.getMatchedSkillsJson()), readJson(entity.getMissingPreferredSkillsJson())),
                entity.getExperienceMatchLabel(),
                "If you genuinely have a missing skill but it isn't reflected here, consider adding it to your resume. Never claim a skill you don't actually have.",
                entity.getCreatedAt()
        );
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize job match data", e);
        }
    }

    private List<String> readJson(String json) {
        try {
            if (json == null) return List.of();
            var type = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}

