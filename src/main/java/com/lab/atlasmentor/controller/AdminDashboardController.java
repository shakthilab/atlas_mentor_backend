package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT', " +
        "'SENIOR_COUNSELLOR', 'JUNIOR_COUNSELLOR')")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * ?period=7d|15d|30d|60d|90d|1y|5y  (default: 30d)
     */
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getKpiCards(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("KPI cards retrieved successfully",
                adminDashboardService.getKpiCards(period)));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<DashboardStudentsResponse>> getStudentsSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Students dashboard retrieved successfully",
                adminDashboardService.getStudentsSection(period)));
    }

    @GetMapping("/financial")
    public ResponseEntity<ApiResponse<DashboardFinancialResponse>> getFinancialSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Financial dashboard retrieved successfully",
                adminDashboardService.getFinancialSection(period)));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<DashboardTasksResponse>> getTasksSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Tasks dashboard retrieved successfully",
                adminDashboardService.getTasksSection(period)));
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<DashboardTeamResponse>> getTeamSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Team dashboard retrieved successfully",
                adminDashboardService.getTeamSection(period)));
    }

    @GetMapping("/referrals")
    public ResponseEntity<ApiResponse<DashboardReferralsResponse>> getReferralsSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Referrals dashboard retrieved successfully",
                adminDashboardService.getReferralsSection(period)));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<DashboardAuditResponse>> getAuditSection(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success("Audit dashboard retrieved successfully",
                adminDashboardService.getAuditSection(period)));
    }
}
