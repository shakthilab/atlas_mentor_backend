package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.DayDetailResponse;
import com.lab.atlasmentor.dto.NeedsAttentionResponse;
import com.lab.atlasmentor.service.DayWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Employee-side Day Approval Workflow actions (Part D3). New - no equivalent endpoint
 * existed; DayWorkspace/DayApproval had models+repositories but no service/controller
 * layer at all before this change (see report).
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class DayWorkspaceController {

    private final DayWorkspaceService dayWorkspaceService;

    @PostMapping("/{employeeId}/days/{date}/submit")
    public ResponseEntity<ApiResponse<DayDetailResponse>> submitDay(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Submit day request: employee={}, date={}", employeeId, date);
        DayDetailResponse response = dayWorkspaceService.submitDay(employeeId, date);
        return ResponseEntity.ok(ApiResponse.success("Day submitted for Branch Partner review", response));
    }

    /**
     * Send-back / rework spec, Part 2. Cross-date by design - a task flagged on a prior
     * day stays here (and stays editable via POST /api/tasks/{taskId}/resubmit) until the
     * employee resubmits it, regardless of how many days have passed since.
     */
    @GetMapping("/{employeeId}/needs-attention")
    public ResponseEntity<ApiResponse<List<NeedsAttentionResponse>>> getNeedsAttention(@PathVariable Long employeeId) {
        log.info("Needs-attention request for employee {}", employeeId);
        List<NeedsAttentionResponse> items = dayWorkspaceService.getNeedsAttention(employeeId);
        if (items.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", items));
        }
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
