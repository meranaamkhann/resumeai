package com.resumeai.service.analysis;

public record Issue(
        String category,
        Severity severity,
        String title,
        String explanation,
        String recommendation
) {
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, GOOD }
}

