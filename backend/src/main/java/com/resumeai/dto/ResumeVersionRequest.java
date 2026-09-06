package com.resumeai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResumeVersionRequest(
        @NotNull UUID documentId,
        @Size(max = 255) String targetRole,
        @Size(max = 255) String targetCompany,
        UUID jobDescriptionId
) {}

