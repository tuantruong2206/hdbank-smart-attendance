package com.hdbank.report.controller;

import com.hdbank.report.service.DashboardService;
import com.hdbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard/metrics — role-based real metrics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics(
            @RequestParam(required = false) UUID organizationId,
            @RequestHeader(value = "X-User-Role", defaultValue = "EMPLOYEE") String role) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getMetrics(organizationId, role)));
    }

    /**
     * GET /api/v1/dashboard/metrics/sse — SSE with real-time updates.
     */
    @GetMapping(value = "/metrics/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        return dashboardService.createSseEmitter();
    }

    /**
     * GET /api/v1/dashboard/team-pulse — manager's team today status.
     */
    @GetMapping("/team-pulse")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTeamPulse(
            @RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTeamPulse(organizationId)));
    }

    /**
     * GET /api/v1/dashboard/branch-comparison — executive branch comparison.
     */
    @GetMapping("/branch-comparison")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBranchComparison(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getBranchComparison(date)));
    }
}
