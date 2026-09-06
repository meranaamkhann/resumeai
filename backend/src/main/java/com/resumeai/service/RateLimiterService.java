package com.resumeai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.resumeai.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    private final int uploadPerMinute;
    private final int analysisPerMinute;
    private final int authPerMinute;

    public RateLimiterService(
            @Value("${app.rate-limit.upload-per-minute}") int uploadPerMinute,
            @Value("${app.rate-limit.analysis-per-minute}") int analysisPerMinute,
            @Value("${app.rate-limit.auth-per-minute}") int authPerMinute) {
        this.uploadPerMinute = uploadPerMinute;
        this.analysisPerMinute = analysisPerMinute;
        this.authPerMinute = authPerMinute;
    }

    public void checkUpload(String key) {
        check("upload:" + key, uploadPerMinute, "Too many uploads. Please wait a moment before trying again.");
    }

    public void checkAnalysis(String key) {
        check("analysis:" + key, analysisPerMinute, "Too many analysis requests. Please wait a moment before trying again.");
    }

    public void checkAuth(String key) {
        check("auth:" + key, authPerMinute, "Too many attempts. Please wait a moment before trying again.");
    }

    private void check(String bucketKey, int perMinute, String message) {
        Bucket bucket = buckets.get(bucketKey, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(perMinute, Refill.greedy(perMinute, Duration.ofMinutes(1))))
                .build());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(message);
        }
    }
}

