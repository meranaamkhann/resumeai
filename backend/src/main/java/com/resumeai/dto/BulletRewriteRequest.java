package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BulletRewriteRequest(
        @NotBlank @Size(max = 500) String bulletText,
        @NotBlank @Pattern(regexp = "ATS_OPTIMIZED|RECRUITER_FRIENDLY|CONCISE|TECHNICAL|ACHIEVEMENT_FOCUSED") String mode
) {}

