package com.resumeai.service.ai;

import com.resumeai.dto.BulletRewriteRequest;
import com.resumeai.dto.BulletRewriteResponse;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.repository.UserRepository;
import com.resumeai.service.AnalyticsService;
import com.resumeai.service.billing.UsageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BulletRewriteService {

    private final AiAnalysisService aiAnalysisService;
    private final FactualConsistencyGuard guard;
    private final UsageService usageService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;

    public BulletRewriteService(AiAnalysisService aiAnalysisService, FactualConsistencyGuard guard,
                                 UsageService usageService, UserRepository userRepository,
                                 AnalyticsService analyticsService) {
        this.aiAnalysisService = aiAnalysisService;
        this.guard = guard;
        this.usageService = usageService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public BulletRewriteResponse rewrite(UUID userId, BulletRewriteRequest request) {
        if (!aiAnalysisService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI-powered rewriting is not enabled on this deployment. Set AI_PROVIDER and AI_API_KEY to use this feature.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Account not found"));
        usageService.checkAndIncrement(userId, user.getPlan(), UsageService.UsageType.OPTIMIZATION);

        String rewritten;
        try {
            rewritten = aiAnalysisService.rewriteBullet(request.bulletText(), request.mode());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                    "The AI provider failed to respond. Please try again.");
        }

        FactualConsistencyGuard.GuardResult guardResult = guard.check(request.bulletText(), rewritten);

        analyticsService.record(userId, guardResult.passed() ? "bullet_rewritten" : "bullet_rewrite_rejected");

        if (!guardResult.passed()) {
            return new BulletRewriteResponse(request.bulletText(), request.bulletText(), false, guardResult.reason());
        }

        return new BulletRewriteResponse(request.bulletText(), rewritten, true, null);
    }
}
