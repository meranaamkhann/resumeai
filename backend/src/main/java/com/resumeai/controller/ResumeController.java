package com.resumeai.controller;

import com.resumeai.dto.ResumeUploadResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.RateLimiterService;
import com.resumeai.service.ResumeDeletionService;
import com.resumeai.service.ResumeUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeUploadService resumeUploadService;
    private final ResumeDeletionService resumeDeletionService;
    private final RateLimiterService rateLimiterService;

    public ResumeController(ResumeUploadService resumeUploadService,
                             ResumeDeletionService resumeDeletionService,
                             RateLimiterService rateLimiterService) {
        this.resumeUploadService = resumeUploadService;
        this.resumeDeletionService = resumeDeletionService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ResumeUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", required = false) String label) {
        var user = CurrentUser.get();
        rateLimiterService.checkUpload(user.userId().toString());
        return ResponseEntity.ok(resumeUploadService.upload(user.userId(), label, file));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID resumeId) {
        var user = CurrentUser.get();
        resumeDeletionService.deleteResume(user.userId(), resumeId);
        return ResponseEntity.noContent().build();
    }
}

