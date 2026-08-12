package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.DayDetailResponse;
import com.lab.atlasmentor.dto.DayNodeResponse;
import com.lab.atlasmentor.dto.MonthNodeResponse;
import com.lab.atlasmentor.dto.YearNodeResponse;
import com.lab.atlasmentor.security.SecurityUtils;
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
 * "Me" equivalent of EmployeeTreeController's Years/Months/Days/day-detail endpoints, for
 * individual-contributor roles (SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR, VIDEO_EDITOR, etc.) who
 * have no Branch → Role → Employee tree to navigate at all - for them it's always "me," so the
 * employee identity is resolved from the JWT (SecurityUtils.getCurrentUserId()), never accepted
 * as a path/query param. Delegates to the exact same EmployeeTreeService methods
 * /api/admin/employees/{employeeId}/... uses, so behavior (including AccessScopeService's
 * self-only check, which trivially passes here since employeeId == caller) never drifts between
 * the two.
 *
 * <p>Open to any authenticated role, not just individual contributors - there's no reason to
 * block an ADMIN/MANAGER/BRANCH_PARTNER from viewing their own data this way too, and the
 * service-level self-check makes it safe regardless of role.
 */
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class MyWorkspaceController {

    private final EmployeeTreeService employeeTreeService;

    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<YearNodeResponse>>> getMyYears() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        List<YearNodeResponse> years = employeeTreeService.getEmployeeYears(employeeId);
        if (years.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", years));
        }
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/years/{year}/months")
    public ResponseEntity<ApiResponse<List<MonthNodeResponse>>> getMyMonths(@PathVariable int year) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        List<MonthNodeResponse> months = employeeTreeService.getEmployeeMonths(employeeId, year);
        if (months.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", months));
        }
        return ResponseEntity.ok(ApiResponse.success(months));
    }

    @GetMapping("/years/{year}/months/{month}/days")
    public ResponseEntity<ApiResponse<List<DayNodeResponse>>> getMyDays(
            @PathVariable int year, @PathVariable int month) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        List<DayNodeResponse> days = employeeTreeService.getEmployeeDays(employeeId, year, month);
        if (days.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", days));
        }
        return ResponseEntity.ok(ApiResponse.success(days));
    }

    @GetMapping("/days/{date}")
    public ResponseEntity<ApiResponse<DayDetailResponse>> getMyDayDetail(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(employeeTreeService.getDayDetail(employeeId, date)));
    }
}
