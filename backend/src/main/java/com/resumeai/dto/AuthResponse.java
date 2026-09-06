package com.resumeai.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String fullName
) {}

