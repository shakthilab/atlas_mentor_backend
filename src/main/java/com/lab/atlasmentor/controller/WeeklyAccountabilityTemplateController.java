package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateWeekRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateStatusRequest;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.WeeklyAccountabilityTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin CRUD for Weekly Accountability Templates - mirrors {@link RoleTemplateController}'s API shape. */
@RestController
@RequestMapping("/api/weekly-accountability-templates")
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityTemplateController {

    private final WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> createTemplate(
            @Valid @RequestBody WeeklyAccountabilityTemplateRequest request) {
        log.info("REST request to create weekly accountability template: {}", request.getName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService.createTemplate(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> getTemplateById(@PathVariable Long id) {
        log.info("REST request to get weekly accountability template ID: {}", id);
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WeeklyAccountabilityTemplateResponse>>> listTemplates(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) BundleStatus status) {
        log.info("REST request to list weekly accountability templates - roleId: {}, status: {}", roleId, status);
        List<WeeklyAccountabilityTemplateResponse> response = weeklyAccountabilityTemplateService.listTemplates(roleId, status);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability templates retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody WeeklyAccountabilityTemplateRequest request) {
        log.info("REST request to update weekly accountability template ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService.updateTemplate(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template updated successfully", response));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> publishTemplate(@PathVariable Long id) {
        log.info("REST request to publish weekly accountability template ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService.publishTemplate(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template published successfully", response));
    }

    /**
     * Single endpoint to activate or deactivate an already-published template - the
     * request body's status (ACTIVE or INACTIVE) says which way to flip it. Mirrors
     * {@code RoleTemplateController#updateTemplateStatus}.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> updateTemplateStatus(
            @PathVariable Long id,
            @Valid @RequestBody WeeklyAccountabilityTemplateStatusRequest request) {
        log.info("REST request to set weekly accountability template ID: {} status to: {}", id, request.getStatus());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService
                .updateTemplateStatus(id, request.getStatus(), currentUserId);
        String message = request.getStatus() == BundleStatus.ACTIVE
                ? "Weekly accountability template activated successfully"
                : "Weekly accountability template deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        log.info("REST request to delete weekly accountability template ID: {}", id);
        weeklyAccountabilityTemplateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template deleted successfully", null));
    }

    /**
     * Duplicates an entire template (all its weeks and questions) into a brand new,
     * independent template - separate from {@link #duplicateWeek}, which copies one week's
     * questions into another week within a single existing template.
     */
    @PostMapping("/{templateId}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> duplicateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody WeeklyAccountabilityDuplicateTemplateRequest request) {
        log.info("REST request to duplicate weekly accountability template ID: {} as '{}'", templateId, request.getNewTemplateName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService
                .duplicateTemplate(templateId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Weekly accountability template duplicated successfully", response));
    }

    /**
     * Copies {@code sourceWeekNumber}'s questions into {@code weekNumber} - REPLACES the
     * target week's current questions, does not append. Only valid within this same template.
     */
    @PostMapping("/{id}/weeks/{weekNumber}/duplicate-from")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyAccountabilityTemplateResponse>> duplicateWeek(
            @PathVariable Long id,
            @PathVariable Integer weekNumber,
            @Valid @RequestBody WeeklyAccountabilityDuplicateWeekRequest request) {
        log.info("REST request to duplicate weekly accountability template {} week {} from week {}",
                id, weekNumber, request.getSourceWeekNumber());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WeeklyAccountabilityTemplateResponse response = weeklyAccountabilityTemplateService
                .duplicateWeek(id, weekNumber, request.getSourceWeekNumber(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Week duplicated successfully", response));
    }
}
