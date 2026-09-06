package com.resumeai.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email) {
}

