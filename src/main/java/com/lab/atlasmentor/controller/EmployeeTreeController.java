package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.service.EmployeeTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Employee Tree navigation (Part A) and the admin Daily Workspace day-detail panel
 * (Part B). Years/Months/Days (A2/A3) are genuinely data-driven: every query goes through
 * DayWorkspaceRepository against real day_workspaces rows, never a static Jan-Dec walk.
 *
 * <p>Open to every role EXCEPT individual contributors (SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR,
 * VIDEO_EDITOR, etc.) - they have no "browse other employees" concept at all and get a 403 here
 * by design; see MyWorkspaceController for their own "me" equivalent of every endpoint below.
 * ADMIN/ADMINISTRATIVE_ASSISTANT/MANAGER see everything unfiltered; BRANCH_PARTNER is scoped to
 * their own branch by AccessScopeService inside EmployeeTreeService, not here - this annotation
 * only gates "which roles may call this controller at all," not per-branch/per-employee scoping.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVE_ASSISTANT', 'MANAGER', 'BRANCH_PARTNER')")
@Slf4j
public class EmployeeTreeController {

    private final EmployeeTreeService employeeTreeService;

    @GetMapping("/employee-tree")
    public ResponseEntity<ApiResponse<EmployeeTreeResponse>> getEmployeeTree() {
        return ResponseEntity.ok(ApiResponse.success(employeeTreeService.getEmployeeTree()));
    }

    @GetMapping("/employees/{employeeId}/years")
    public ResponseEntity<ApiResponse<List<YearNodeResponse>>> getEmployeeYears(@PathVariable Long employeeId) {
        List<YearNodeResponse> years = employeeTreeService.getEmployeeYears(employeeId);
        if (years.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", years));
        }
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/employees/{employeeId}/years/{year}/months")
    public ResponseEntity<ApiResponse<List<MonthNodeResponse>>> getEmployeeMonths(
            @PathVariable Long employeeId, @PathVariable int year) {
        List<MonthNodeResponse> months = employeeTreeService.getEmployeeMonths(employeeId, year);
        if (months.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", months));
        }
        return ResponseEntity.ok(ApiResponse.success(months));
    }

    @GetMapping("/employees/{employeeId}/years/{year}/months/{month}/days")
    public ResponseEntity<ApiResponse<List<DayNodeResponse>>> getEmployeeDays(
            @PathVariable Long employeeId, @PathVariable int year, @PathVariable int month) {
        List<DayNodeResponse> days = employeeTreeService.getEmployeeDays(employeeId, year, month);
        if (days.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", days));
        }
        return ResponseEntity.ok(ApiResponse.success(days));
    }

    @GetMapping("/employees/{employeeId}/days/{date}")
    public ResponseEntity<ApiResponse<DayDetailResponse>> getDayDetail(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(employeeTreeService.getDayDetail(employeeId, date)));
    }
}
