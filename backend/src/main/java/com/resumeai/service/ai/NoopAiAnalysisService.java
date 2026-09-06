package com.resumeai.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "none", matchIfMissing = true)
public class NoopAiAnalysisService implements AiAnalysisService {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String rewriteBullet(String originalBullet, String mode) {
        throw new UnsupportedOperationException(
                "AI-powered rewriting is not configured on this deployment (AI_PROVIDER=none). " +
                "Set AI_PROVIDER and AI_API_KEY to enable it.");
    }
}

