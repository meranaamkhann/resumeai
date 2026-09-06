package com.resumeai.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        String jobUrl,
        String location,
        LocalDate applicationDate,
        UUID resumeVersionId,
        String status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}

