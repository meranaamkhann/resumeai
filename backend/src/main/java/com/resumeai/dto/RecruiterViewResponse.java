package com.resumeai.dto;

import java.util.List;

public record RecruiterViewResponse(
        String firstImpression,
        List<String> strengths,
        List<String> concerns,
        String label
) {}

