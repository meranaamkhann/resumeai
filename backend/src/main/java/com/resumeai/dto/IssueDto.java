package com.resumeai.dto;

public record IssueDto(
        String category,
        String severity,
        String title,
        String explanation,
        String recommendation
) {}

