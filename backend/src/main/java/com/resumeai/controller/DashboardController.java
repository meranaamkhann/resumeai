package com.resumeai.controller;

import com.resumeai.dto.DashboardResponse;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        var user = CurrentUser.get();
        return ResponseEntity.ok(dashboardService.getDashboard(user.userId()));
    }
}

