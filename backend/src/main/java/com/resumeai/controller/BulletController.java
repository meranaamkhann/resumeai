package com.resumeai.controller;

import com.resumeai.dto.BulletRewriteRequest;
import com.resumeai.dto.BulletRewriteResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.RateLimiterService;
import com.resumeai.service.ai.BulletRewriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bullets")
public class BulletController {

    private final BulletRewriteService bulletRewriteService;
    private final RateLimiterService rateLimiterService;

    public BulletController(BulletRewriteService bulletRewriteService, RateLimiterService rateLimiterService) {
        this.bulletRewriteService = bulletRewriteService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/rewrite")
    public ResponseEntity<BulletRewriteResponse> rewrite(@Valid @RequestBody BulletRewriteRequest request) {
        var user = CurrentUser.get();
        rateLimiterService.checkAnalysis(user.userId().toString());
        return ResponseEntity.ok(bulletRewriteService.rewrite(user.userId(), request));
    }
}

