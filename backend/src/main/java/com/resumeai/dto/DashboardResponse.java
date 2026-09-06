package com.resumeai.dto;

import java.util.List;

public record DashboardResponse(
        Integer latestOverallScore,
        Integer resumeCount,
        Integer analysisCount,
        List<String> topImprovementAreas,
        List<RecentAnalysisSummary> recentAnalyses
) {
    public record RecentAnalysisSummary(
            java.util.UUID analysisId,
            java.util.UUID resumeId,
            java.util.UUID documentId,
            int overallScore,
            java.time.Instant createdAt
    ) {}
}

