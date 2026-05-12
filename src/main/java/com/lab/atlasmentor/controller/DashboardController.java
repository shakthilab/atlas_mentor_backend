package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.CommissionTrendResponse;
import com.lab.atlasmentor.dto.ReferralSummaryResponse;
import com.lab.atlasmentor.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/referral-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ReferralSummaryResponse> getReferralSummary() {
        return ResponseEntity.ok(dashboardService.getReferralSummary());
    }

    @GetMapping("/referral-commission-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<CommissionTrendResponse> getCommissionTrend(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dashboardService.getCommissionTrend(range, from, to));
    }
}