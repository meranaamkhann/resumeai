package com.resumeai.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Component
public class OpaqueTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public record Token(String rawValue, String hash, Instant expiresAt) {}

    public Token generate(long ttlMinutes) {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return new Token(raw, hash(raw), Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

