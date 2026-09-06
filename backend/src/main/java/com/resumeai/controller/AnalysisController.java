package com.resumeai.controller;

import com.resumeai.dto.AnalysisResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.AnalysisService;
import com.resumeai.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final RateLimiterService rateLimiterService;

    public AnalysisController(AnalysisService analysisService, RateLimiterService rateLimiterService) {
        this.analysisService = analysisService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/documents/{documentId}")
    public ResponseEntity<AnalysisResponse> analyze(@PathVariable UUID documentId) {
        var user = CurrentUser.get();
        rateLimiterService.checkAnalysis(user.userId().toString());
        return ResponseEntity.ok(analysisService.analyze(user.userId(), documentId));
    }

    @GetMapping("/documents/{documentId}/latest")
    public ResponseEntity<AnalysisResponse> latest(@PathVariable UUID documentId) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(analysisService.getLatestForDocument(user.userId(), documentId));
    }
}

