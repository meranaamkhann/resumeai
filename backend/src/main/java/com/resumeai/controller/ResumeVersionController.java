package com.resumeai.controller;

import com.resumeai.dto.ResumeVersionRequest;
import com.resumeai.dto.ResumeVersionResponse;
import com.resumeai.dto.ScoreComparisonResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.ResumeVersionService;
import com.resumeai.service.ScoreComparisonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume-versions")
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;
    private final ScoreComparisonService scoreComparisonService;

    public ResumeVersionController(ResumeVersionService resumeVersionService, ScoreComparisonService scoreComparisonService) {
        this.resumeVersionService = resumeVersionService;
        this.scoreComparisonService = scoreComparisonService;
    }

    @PostMapping
    public ResponseEntity<ResumeVersionResponse> create(@Valid @RequestBody ResumeVersionRequest request) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(resumeVersionService.create(user.userId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ResumeVersionResponse>> list() {
        var user = CurrentUser.get();
        return ResponseEntity.ok(resumeVersionService.list(user.userId()));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<ResumeVersionResponse> get(@PathVariable UUID versionId) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(resumeVersionService.get(user.userId(), versionId));
    }

    @GetMapping("/compare/{previousAnalysisId}/{newAnalysisId}")
    public ResponseEntity<ScoreComparisonResponse> compare(@PathVariable UUID previousAnalysisId, @PathVariable UUID newAnalysisId) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(scoreComparisonService.compare(user.userId(), previousAnalysisId, newAnalysisId));
    }
}

