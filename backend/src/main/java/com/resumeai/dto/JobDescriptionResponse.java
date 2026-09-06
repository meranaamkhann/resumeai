package com.resumeai.dto;

import java.util.List;
import java.util.UUID;

public record JobDescriptionResponse(
        UUID id,
        String jobTitle,
        String seniority,
        Integer minYearsExperience,
        List<String> requiredSkills,
        List<String> preferredSkills
) {}

