package com.resumeai.controller;

import com.resumeai.dto.ApplicationRequest;
import com.resumeai.dto.ApplicationResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.ApplicationTrackerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationTrackerService applicationTrackerService;

    public ApplicationController(ApplicationTrackerService applicationTrackerService) {
        this.applicationTrackerService = applicationTrackerService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest request) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(applicationTrackerService.create(user.userId(), request));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> update(@PathVariable UUID applicationId, @Valid @RequestBody ApplicationRequest request) {
        var user = CurrentUser.get();
        return ResponseEntity.ok(applicationTrackerService.update(user.userId(), applicationId, request));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> list() {
        var user = CurrentUser.get();
        return ResponseEntity.ok(applicationTrackerService.list(user.userId()));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID applicationId) {
        var user = CurrentUser.get();
        applicationTrackerService.delete(user.userId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}

