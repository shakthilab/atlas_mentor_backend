package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.RoleTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-templates")
@RequiredArgsConstructor
@Slf4j
public class RoleTemplateController {

    private final RoleTemplateService roleTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> createTemplate(
            @Valid @RequestBody RoleTemplateRequest request) {
        log.info("REST request to create role template: {}", request.getName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.createTemplate(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> getTemplateById(@PathVariable Long id) {
        log.info("REST request to get role template ID: {}", id);
        RoleTemplateResponse response = roleTemplateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Role template retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleTemplateResponse>>> listTemplates(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) BundleStatus status) {
        log.info("REST request to list role templates - roleId: {}, branchId: {}, status: {}", roleId, branchId, status);
        List<RoleTemplateResponse> response = roleTemplateService.listTemplates(roleId, branchId, status);
        return ResponseEntity.ok(ApiResponse.success("Role templates retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RoleTemplateRequest request) {
        log.info("REST request to update role template ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.updateTemplate(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template updated successfully", response));
    }

    @PostMapping("/{templateId}/days/{dayNumber}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateTaskResponse>> addTaskToDay(
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @Valid @RequestBody RoleTemplateTaskRequest request) {
        log.info("REST request to add task to template ID: {}, day: {}, month: {}, year: {}", templateId, dayNumber, month, year);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateTaskResponse response = roleTemplateService.addTaskToDay(templateId, dayNumber, month, year, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Task added successfully", response));
    }

    @PutMapping("/{templateId}/days/{dayNumber}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateTaskResponse>> updateTaskInDay(
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @PathVariable Long taskId,
            @Valid @RequestBody RoleTemplateTaskRequest request) {
        log.info("REST request to update task ID: {} on template ID: {}, day: {}", taskId, templateId, dayNumber);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateTaskResponse response = roleTemplateService.updateTaskInDay(templateId, dayNumber, taskId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    @DeleteMapping("/{templateId}/days/{dayNumber}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTaskFromDay(
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @PathVariable Long taskId) {
        log.info("REST request to delete task ID: {} on template ID: {}, day: {}", taskId, templateId, dayNumber);
        roleTemplateService.deleteTaskFromDay(templateId, dayNumber, taskId);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PostMapping("/{id}/days/{dayId}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> duplicateDayTasks(
            @PathVariable Long id,
            @PathVariable Long dayId,
            @Valid @RequestBody DuplicateDayRequest request) {
        log.info("REST request to duplicate day ID: {} under template ID: {}", dayId, id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.duplicateDayTasks(id, dayId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Day tasks duplicated successfully", response));
    }

    /**
     * Duplicates an entire template (all its days and tasks) into a brand new, independent
     * template. Separate from {@link #duplicateDayTasks}, which copies a day's tasks within
     * a single existing template.
     */
    @PostMapping("/{templateId}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> duplicateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody DuplicateTemplateRequest request) {
        log.info("REST request to duplicate role template ID: {} as '{}'", templateId, request.getNewTemplateName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.duplicateTemplate(templateId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template duplicated successfully", response));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> publishTemplate(@PathVariable Long id) {
        log.info("REST request to publish role template ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.publishTemplate(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template published successfully", response));
    }

    /**
     * Single endpoint to activate or deactivate an already-published template - the
     * request body's status (ACTIVE or INACTIVE) says which way to flip it.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> updateTemplateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoleTemplateStatusRequest request) {
        log.info("REST request to set role template ID: {} status to: {}", id, request.getStatus());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.updateTemplateStatus(id, request.getStatus(), currentUserId);
        String message = request.getStatus() == BundleStatus.ACTIVE
                ? "Role template activated successfully"
                : "Role template deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        log.info("REST request to delete role template ID: {}", id);
        roleTemplateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Role template deleted successfully", null));
    }

    /**
     * Permanently deletes the template row along with all its days/tasks - not a soft
     * delete. Irreversible; kept as a separate endpoint from {@link #deleteTemplate} so
     * callers must opt into it explicitly.
     */
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteTemplate(@PathVariable Long id) {
        log.info("REST request to hard delete role template ID: {}", id);
        roleTemplateService.hardDeleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Role template permanently deleted", null));
    }
}
