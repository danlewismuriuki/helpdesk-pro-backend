package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.DashboardStatsResponse;
import com.helpdeskpro.backend.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Controller
 * Provides administrative endpoints for dashboard and analytics
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Administrative endpoints for dashboard and analytics")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminStatsService adminStatsService;

    /**
     * Get complete dashboard statistics
     *
     * GET /api/v1/admin/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
            summary = "Get dashboard statistics",
            description = "Retrieve comprehensive dashboard statistics including ticket metrics, agent performance, and knowledge base stats"
    )
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        log.info("Fetching dashboard statistics");

        DashboardStatsResponse stats = adminStatsService.getDashboardStats();

        return ResponseEntity.ok(
                ApiResponse.success(stats, "Dashboard statistics retrieved successfully")
        );
    }
}