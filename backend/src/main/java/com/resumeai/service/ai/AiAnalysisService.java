package com.resumeai.service.ai;

public interface AiAnalysisService {

    boolean isConfigured();

    String rewriteBullet(String originalBullet, String mode);
}

