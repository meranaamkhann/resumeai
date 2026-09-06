package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ApplicationRequest(
        @NotBlank @Size(max = 255) String company,
        @NotBlank @Size(max = 255) String role,
        @Size(max = 1000) String jobUrl,
        @Size(max = 255) String location,
        LocalDate applicationDate,
        UUID resumeVersionId,
        String status,
        @Size(max = 5000) String notes
) {}

