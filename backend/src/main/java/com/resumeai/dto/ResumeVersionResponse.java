package com.resumeai.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeVersionResponse(
        UUID id,
        UUID resumeId,
        UUID documentId,
        String targetRole,
        String targetCompany,
        UUID jobDescriptionId,
        Instant createdAt,
        Instant updatedAt
) {}

