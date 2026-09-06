package com.resumeai.controller;

import com.resumeai.dto.JobDescriptionRequest;
import com.resumeai.dto.JobDescriptionResponse;
import com.resumeai.dto.JobMatchResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.RateLimiterService;
import com.resumeai.service.matching.JobMatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final RateLimiterService rateLimiterService;

    public JobMatchController(JobMatchService jobMatchService, RateLimiterService rateLimiterService) {
        this.jobMatchService = jobMatchService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/job-descriptions")
    public ResponseEntity<JobDescriptionResponse> submit(@Valid @RequestBody JobDescriptionRequest request) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(jobMatchService.submitJobDescription(user.userId(), request));
    }

    @PostMapping("/job-match/{documentId}/{jobDescriptionId}")
    public ResponseEntity<JobMatchResponse> match(@PathVariable UUID documentId, @PathVariable UUID jobDescriptionId) {
        var user = CurrentUser.get();
        rateLimiterService.checkAnalysis(user.userId().toString());
        return ResponseEntity.ok(jobMatchService.match(user.userId(), documentId, jobDescriptionId));
    }
}

