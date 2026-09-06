package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobDescriptionRequest(
        @NotBlank @Size(max = 20000) String rawText
) {}

