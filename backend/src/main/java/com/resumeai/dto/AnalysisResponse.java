package com.resumeai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
        UUID analysisId,
        UUID documentId,
        int overallScore,
        CategoryScores categoryScores,
        List<IssueDto> issues,
        List<String> detectedSections,
        String disclaimer,
        Instant createdAt
) {
    public record CategoryScores(
            int atsCompatibility,
            int keywordAlignment,
            int structure,
            int contentQuality,
            int experienceRelevance,
            int impact,
            int formatting
    ) {}
}

