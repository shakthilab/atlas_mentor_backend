package com.lab.atlasmentor.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.IdempotencyService;
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
    private final IdempotencyService idempotencyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> createTemplate(
            @Valid @RequestBody RoleTemplateRequest request) {
        log.info("REST request to create role template: {}", request.getName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.createTemplate(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> getTemplateById(@PathVariable Long id) {
        log.info("REST request to get role template ID: {}", id);
        RoleTemplateResponse response = roleTemplateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Role template retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<List<RoleTemplateResponse>>> listTemplates(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) BundleStatus status) {
        log.info("REST request to list role templates - roleId: {}, branchId: {}, status: {}", roleId, branchId, status);
        List<RoleTemplateResponse> response = roleTemplateService.listTemplates(roleId, branchId, status);
        return ResponseEntity.ok(ApiResponse.success("Role templates retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RoleTemplateRequest request) {
        log.info("REST request to update role template ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.updateTemplate(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template updated successfully", response));
    }

    /**
     * Handles both single-task and bulk/multi-day task creation, distinguished purely by the
     * request body (see {@link RoleTemplateTaskRequest}) - same URL, same auth, so existing
     * single-task callers are unaffected:
     * - Single mode (request.tasks empty/absent): unchanged behavior, returns one
     *   RoleTemplateTaskResponse.
     * - Bulk mode (request.tasks non-empty): clones that task list onto the URL's own day plus
     *   every day in request.targetDays, all in one transaction - so a UI clone action that used
     *   to fire one POST per task per day (and could leave a day partially populated if one call
     *   failed mid-loop) becomes a single all-or-nothing call. Returns one
     *   RoleTemplateBulkTaskResponse per target day.
     *
     * Bulk mode also accepts an optional {@code Idempotency-Key} header. Cloning is
     * deliberately additive (see RoleTemplateService.duplicateTasksToDay), which means a
     * network retry or a resubmitted request duplicates every task again with no way to tell
     * it apart from a genuine second clone - the same failure shape as duplicating a day onto
     * itself, just triggered by a repeated request instead of a self-targeted one. A caller
     * that sends the header is protected from that; one that doesn't gets the old, unprotected
     * behavior.
     */
    @PostMapping("/{templateId}/days/{dayNumber}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<Object>> addTaskToDay(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @Valid @RequestBody RoleTemplateTaskRequest request) {
        boolean isBulk = request.getTasks() != null && !request.getTasks().isEmpty();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (isBulk) {
            log.info("REST request to bulk-add {} task(s) to template ID: {}, day: {}, month: {}, year: {}, extra target days: {}",
                    request.getTasks().size(), templateId, dayNumber, month, year,
                    request.getTargetDays() == null ? 0 : request.getTargetDays().size());

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                Object[] requestKey = {templateId, dayNumber, month, year, request};
                IdempotencyService.Outcome outcome =
                        idempotencyService.begin(idempotencyKey, "addTasksToDaysBulk", requestKey, currentUserId);
                if (outcome instanceof IdempotencyService.Replay replay) {
                    log.info("Idempotency-Key '{}' already processed - replaying cached result instead of cloning again", idempotencyKey);
                    ApiResponse<Object> cached = idempotencyService.replayAs(replay, new TypeReference<ApiResponse<Object>>() {});
                    return ResponseEntity.status(replay.status()).header("Idempotent-Replayed", "true").body(cached);
                }
                long claimId = ((IdempotencyService.Proceed) outcome).rowId();
                List<RoleTemplateBulkTaskResponse> response;
                try {
                    response = roleTemplateService.addTasksToDaysBulk(templateId, dayNumber, month, year, request, currentUserId);
                } catch (RuntimeException ex) {
                    // Nothing was persisted - release the claim so a corrected retry with the
                    // same key can actually run, instead of being stuck at "still processing".
                    idempotencyService.releaseOnFailure(claimId);
                    throw ex;
                }
                ApiResponse<Object> body = ApiResponse.success("Tasks cloned successfully", response);
                idempotencyService.complete(claimId, 200, body);
                return ResponseEntity.ok(body);
            }

            List<RoleTemplateBulkTaskResponse> response =
                    roleTemplateService.addTasksToDaysBulk(templateId, dayNumber, month, year, request, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Tasks cloned successfully", response));
        }

        log.info("REST request to add task to template ID: {}, day: {}, month: {}, year: {}", templateId, dayNumber, month, year);
        RoleTemplateTaskResponse response = roleTemplateService.addTaskToDay(templateId, dayNumber, month, year, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Task added successfully", response));
    }

    @PutMapping("/{templateId}/days/{dayNumber}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<Void>> deleteTaskFromDay(
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @PathVariable Long taskId) {
        log.info("REST request to delete task ID: {} on template ID: {}, day: {}", taskId, templateId, dayNumber);
        roleTemplateService.deleteTaskFromDay(templateId, dayNumber, taskId);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    /**
     * Deletes every task on a day in one call. month/year work the same as on
     * {@link #addTaskToDay}: omit both to target the recurring day for this dayNumber, or
     * pass both to target the day scoped to that specific month. Never creates a day - if
     * none matches, there's nothing to delete and this is a no-op.
     */
    @DeleteMapping("/{templateId}/days/{dayNumber}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<Void>> deleteAllTasksFromDay(
            @PathVariable Long templateId,
            @PathVariable Integer dayNumber,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        log.info("REST request to delete all tasks on template ID: {}, day: {}, month: {}, year: {}", templateId, dayNumber, month, year);
        roleTemplateService.deleteAllTasksFromDay(templateId, dayNumber, month, year);
        return ResponseEntity.ok(ApiResponse.success("Tasks deleted successfully", null));
    }

    /**
     * Accepts the same optional {@code Idempotency-Key} header as bulk task cloning above,
     * for the same reason: duplication here is additive too (RANGE mode especially fans out
     * to several days per call), so a retried or resubmitted request would otherwise
     * duplicate every task it touches again.
     */
    @PostMapping("/{id}/days/{dayId}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> duplicateDayTasks(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long id,
            @PathVariable Long dayId,
            @Valid @RequestBody DuplicateDayRequest request) {
        log.info("REST request to duplicate day ID: {} under template ID: {}", dayId, id);
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Object[] requestKey = {id, dayId, request};
            IdempotencyService.Outcome outcome =
                    idempotencyService.begin(idempotencyKey, "duplicateDayTasks", requestKey, currentUserId);
            if (outcome instanceof IdempotencyService.Replay replay) {
                log.info("Idempotency-Key '{}' already processed - replaying cached result instead of duplicating again", idempotencyKey);
                ApiResponse<RoleTemplateResponse> cached =
                        idempotencyService.replayAs(replay, new TypeReference<ApiResponse<RoleTemplateResponse>>() {});
                return ResponseEntity.status(replay.status()).header("Idempotent-Replayed", "true").body(cached);
            }
            long claimId = ((IdempotencyService.Proceed) outcome).rowId();
            RoleTemplateResponse response;
            try {
                response = roleTemplateService.duplicateDayTasks(id, dayId, request, currentUserId);
            } catch (RuntimeException ex) {
                idempotencyService.releaseOnFailure(claimId);
                throw ex;
            }
            ApiResponse<RoleTemplateResponse> body = ApiResponse.success("Day tasks duplicated successfully", response);
            idempotencyService.complete(claimId, 200, body);
            return ResponseEntity.ok(body);
        }

        RoleTemplateResponse response = roleTemplateService.duplicateDayTasks(id, dayId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Day tasks duplicated successfully", response));
    }

    /**
     * Duplicates an entire template (all its days and tasks) into a brand new, independent
     * template. Separate from {@link #duplicateDayTasks}, which copies a day's tasks within
     * a single existing template.
     */
    @PostMapping("/{templateId}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<RoleTemplateResponse>> duplicateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody DuplicateTemplateRequest request) {
        log.info("REST request to duplicate role template ID: {} as '{}'", templateId, request.getNewTemplateName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        RoleTemplateResponse response = roleTemplateService.duplicateTemplate(templateId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role template duplicated successfully", response));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
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
