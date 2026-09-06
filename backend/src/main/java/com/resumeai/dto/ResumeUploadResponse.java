package com.resumeai.dto;

import java.util.UUID;

public record ResumeUploadResponse(
        UUID resumeId,
        UUID documentId,
        String status,
        int parsingConfidence,
        boolean parsingConfidenceWarning
) {}

