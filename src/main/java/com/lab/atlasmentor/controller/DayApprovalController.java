package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.DayApprovalActionRequest;
import com.lab.atlasmentor.dto.DayApprovalResponse;
import com.lab.atlasmentor.dto.PendingApprovalResponse;
import com.lab.atlasmentor.dto.ResubmittedTaskResponse;
import com.lab.atlasmentor.service.DayApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Part E - the three-stage Day Approval Workflow (Branch Partner → Branch Manager →
 * Admin). New - the day_workspaces/day_approvals tables and CHECK constraints already
 * existed (V7/V8 migrations) but had no service/controller layer acting on them before
 * this change (see report).
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
@Slf4j
public class DayApprovalController {

    private final DayApprovalService dayApprovalService;

    @GetMapping("/api/approvals/pending")
    public ResponseEntity<ApiResponse<List<PendingApprovalResponse>>> getPendingApprovals(
            @RequestParam(required = false) String stage) {
        List<PendingApprovalResponse> pending = dayApprovalService.getPendingApprovals(stage);
        if (pending.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", pending));
        }
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    /**
     * Send-back / rework spec, Part 5's "review item": tasks the employee already fixed
     * and resubmitted, awaiting re-review by exactly the stage that flagged them (not the
     * day-level pipeline in E1, which never moved for these). Resolve via POST
     * .../approve with that taskId - APPROVE closes the cycle, SEND_BACK re-flags it.
     */
    @GetMapping("/api/approvals/resubmitted")
    public ResponseEntity<ApiResponse<List<ResubmittedTaskResponse>>> getResubmittedTasks(
            @RequestParam(required = false) String stage) {
        List<ResubmittedTaskResponse> resubmitted = dayApprovalService.getResubmittedTasks(stage);
        if (resubmitted.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", resubmitted));
        }
        return ResponseEntity.ok(ApiResponse.success(resubmitted));
    }

    @PostMapping("/api/days/{dayWorkspaceId}/approve")
    public ResponseEntity<ApiResponse<Void>> actOnDay(
            @PathVariable Long dayWorkspaceId,
            @Valid @RequestBody DayApprovalActionRequest request) {
        log.info("Day approval action: dayWorkspaceId={}, action={}", dayWorkspaceId, request.getAction());
        dayApprovalService.actOnDay(dayWorkspaceId, request);
        return ResponseEntity.ok(ApiResponse.success("Action recorded", null));
    }

    // Approval history (E3) is also readable by the employee who owns the day - enforced
    // inside DayApprovalService#getApprovalHistory, not by this class-level role guard, so
    // no @PreAuthorize override is needed here; the service checks reviewer-role-or-owner.
    @GetMapping("/api/days/{dayWorkspaceId}/approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DayApprovalResponse>>> getApprovalHistory(@PathVariable Long dayWorkspaceId) {
        List<DayApprovalResponse> history = dayApprovalService.getApprovalHistory(dayWorkspaceId);
        if (history.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", history));
        }
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
