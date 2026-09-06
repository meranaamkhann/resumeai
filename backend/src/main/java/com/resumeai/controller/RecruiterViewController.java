package com.resumeai.controller;

import com.resumeai.dto.RecruiterViewResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.AnalysisService;
import com.resumeai.service.analysis.RecruiterSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter-view")
public class RecruiterViewController {

    private final AnalysisService analysisService;
    private final RecruiterSimulationService recruiterSimulationService;

    public RecruiterViewController(AnalysisService analysisService, RecruiterSimulationService recruiterSimulationService) {
        this.analysisService = analysisService;
        this.recruiterSimulationService = recruiterSimulationService;
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<RecruiterViewResponse> get(@PathVariable UUID documentId) {
        var user = CurrentUser.get();
        var analysis = analysisService.getLatestForDocument(user.userId(), documentId);
        return ResponseEntity.ok(recruiterSimulationService.generate(analysis));
    }
}

