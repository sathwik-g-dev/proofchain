package com.proofchain.controller;

import com.proofchain.dto.DashboardResponse;
import com.proofchain.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable("userId") Long userId) {
        DashboardResponse dashboard = dashboardService.getDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }
}
