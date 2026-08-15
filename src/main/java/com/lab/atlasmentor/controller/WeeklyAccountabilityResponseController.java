package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityMyTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityResponseSubmitRequest;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.WeeklyAccountabilityResponseService;
import com.lab.atlasmentor.service.WeeklyAccountabilityTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Employee-facing Weekly Accountability reads/writes. Identity always comes from the
 * authenticated JWT (SecurityUtils.getCurrentUserId()) - there is no employeeId path/query
 * parameter anywhere in this controller, so one employee can never read or write another's
 * template view or answers. Open to any authenticated user, same as before.
 */
@RestController
@RequestMapping("/api/weekly-accountability")
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityResponseController {

    private final WeeklyAccountabilityResponseService weeklyAccountabilityResponseService;
    private final WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;

    /**
     * Resolves the caller's role from the JWT-identified user (not a client-supplied param),
     * the current cycle month, and which of the ACTIVE template's 4 weeks today falls into -
     * returning just that week's questions, not all 4. Either "no ACTIVE template for this
     * role this month" or "the current week is unscheduled" is a normal, expected state - not
     * an error - so this returns 200 with {@code data: null} rather than 404 in both cases.
     */
    @GetMapping("/my-template")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityMyTemplateResponse>> getMyTemplate() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        log.info("REST request: employee {} fetching their current weekly accountability checkpoint", employeeId);
        WeeklyAccountabilityMyTemplateResponse response = weeklyAccountabilityTemplateService.getMyTemplate(employeeId);
        String message = response != null
                ? "Active weekly accountability checkpoint retrieved successfully"
                : "No weekly accountability checkpoint scheduled for you right now";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/responses")
    public ResponseEntity<ApiResponse<List<WeeklyAccountabilityAnswerResponse>>> submitResponses(
            @Valid @RequestBody WeeklyAccountabilityResponseSubmitRequest request) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        log.info("REST request: employee {} submitting weekly accountability responses for {}", employeeId, request.getCheckpointDate());
        List<WeeklyAccountabilityAnswerResponse> response = weeklyAccountabilityResponseService.submitResponses(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability responses submitted successfully", response));
    }

    @GetMapping("/responses")
    public ResponseEntity<ApiResponse<List<WeeklyAccountabilityAnswerResponse>>> getResponses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkpointDate) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        log.info("REST request: employee {} fetching weekly accountability responses for {}", employeeId, checkpointDate);
        List<WeeklyAccountabilityAnswerResponse> response = weeklyAccountabilityResponseService.getResponses(employeeId, checkpointDate);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability responses retrieved successfully", response));
    }
}
