package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerResponse;
import com.lab.atlasmentor.security.AccessScopeService;
import com.lab.atlasmentor.service.WeeklyAccountabilityResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Cross-employee read of Weekly Accountability responses, for the same reviewer roles as the
 * Employee Tree ({@link EmployeeTreeController}). Deliberately reuses
 * {@link AccessScopeService#requireEmployeeVisible} rather than introducing a second
 * scoping rule - ADMIN/ADMINISTRATIVE_ASSISTANT/MANAGER see any employee, BRANCH_PARTNER only
 * their own branch (403 otherwise), same split as everywhere else that method gates.
 *
 * <p>The class-level role gate here must stay listed ahead of the generic {@code /api/admin/**}
 * matcher in SecurityConfig (which is ADMIN/SENIOR_COUNSELLOR-only) - see the matching comment
 * there for {@code /api/admin/employee-tree}, the same pitfall applies to this path.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVE_ASSISTANT', 'MANAGER', 'BRANCH_PARTNER')")
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityAdminController {

    private final AccessScopeService accessScopeService;
    private final WeeklyAccountabilityResponseService weeklyAccountabilityResponseService;

    /**
     * Same response shape and "empty array, not 404" convention as the employee's own
     * GET /api/weekly-accountability/responses - this just resolves employeeId from a query
     * param (after the visibility check) instead of the caller's JWT.
     */
    @GetMapping("/weekly-accountability/responses")
    public ResponseEntity<ApiResponse<List<WeeklyAccountabilityAnswerResponse>>> getEmployeeResponses(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkpointDate) {
        log.info("REST request: reviewer fetching weekly accountability responses for employee {} on {}", employeeId, checkpointDate);
        accessScopeService.requireEmployeeVisible(employeeId);
        List<WeeklyAccountabilityAnswerResponse> response = weeklyAccountabilityResponseService.getResponses(employeeId, checkpointDate);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability responses retrieved successfully", response));
    }
}
