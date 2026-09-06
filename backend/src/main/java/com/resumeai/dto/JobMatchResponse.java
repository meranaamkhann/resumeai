package com.resumeai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobMatchResponse(
        UUID matchId,
        UUID documentId,
        UUID jobDescriptionId,
        int matchScore,
        SkillBreakdown requiredSkills,
        SkillBreakdown preferredSkills,
        String experienceMatchLabel,
        String disclaimer,
        Instant createdAt
) {
    public record SkillBreakdown(int matched, int total, List<String> matchedSkills, List<String> missingSkills) {}
}

