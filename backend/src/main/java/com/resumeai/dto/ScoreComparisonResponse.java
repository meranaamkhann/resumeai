package com.resumeai.dto;

public record ScoreComparisonResponse(
        int previousOverallScore,
        int newOverallScore,
        int overallDelta,
        CategoryDelta atsCompatibility,
        CategoryDelta keywordAlignment,
        CategoryDelta structure,
        CategoryDelta contentQuality,
        CategoryDelta experienceRelevance,
        CategoryDelta impact,
        CategoryDelta formatting
) {
    public record CategoryDelta(int previous, int current, int delta) {}
}

