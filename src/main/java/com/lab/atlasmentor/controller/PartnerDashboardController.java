package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.PartnerDashboardResponse;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.PartnerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partners")
public class PartnerDashboardController {

    @Autowired
    private PartnerDashboardService partnerDashboardService;

    /**
     * GET /api/v1/partners/{partnerId}/dashboard?period=30d
     *
     * Supported periods: 7d, 15d, 30d, 60d, 90d, 1y, 5y  (default: 30d)
     *
     * REFERRAL/COMPANY users may only query their own partnerId.
     * ADMIN, MANAGER, BRANCH_PARTNER may query any partner.
     */
    @GetMapping("/{partnerId}/dashboard")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY', 'ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<PartnerDashboardResponse>> getPartnerDashboard(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "30d") String period) {

        String role          = SecurityUtils.getCurrentUserRole();
        Long   currentUserId = SecurityUtils.getCurrentUserId();

        boolean isPartnerRole = "REFERRAL".equalsIgnoreCase(role) || "COMPANY".equalsIgnoreCase(role);
        if (isPartnerRole && !currentUserId.equals(partnerId)) {
            throw new BusinessException("Access denied: you can only view your own dashboard");
        }

        PartnerDashboardResponse data = partnerDashboardService.getDashboard(partnerId, period);
        return ResponseEntity.ok(ApiResponse.success("Partner dashboard retrieved successfully", data));
    }
}
