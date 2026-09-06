package com.resumeai.dto;

public record BulletRewriteResponse(
        String originalBullet,
        String rewrittenBullet,
        boolean factualConsistencyPassed,
        String factualConsistencyNote
) {}

