package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.BundleSchedulerService;
import com.lab.atlasmentor.service.TaskBundleService;
import com.lab.atlasmentor.service.TaskGenerationService;
import com.lab.atlasmentor.service.OverdueTaskSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing task bundles.
 * Provides REST API endpoints for bundle CRUD operations and management.
 */
@RestController
@RequestMapping("/api/task-bundles")
@RequiredArgsConstructor
@Slf4j
public class TaskBundleController {

    private final TaskBundleService taskBundleService;
    private final BundleSchedulerService bundleSchedulerService;
    private final TaskGenerationService taskGenerationService;
    private final OverdueTaskSchedulerService overdueTaskSchedulerService;

    /**
     * Create a new task bundle
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> createTaskBundle(
            @Valid @RequestBody CreateTaskBundleRequest request) {
        log.info("Create task bundle request: {}", request.getName());
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleResponse response = taskBundleService.createTaskBundle(request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all task bundles with pagination and filtering
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundlePageResponse> getTaskBundles(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) BundleStatus status,
            @RequestParam(required = false) com.lab.atlasmentor.enums.ScheduleType scheduleType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Get task bundles with filters: roleId={}, status={}, scheduleType={}, keyword={}", roleId, status, scheduleType, keyword);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        TaskBundlePageResponse response = taskBundleService.getTaskBundles(roleId, status, scheduleType, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get task bundle by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> getTaskBundle(@PathVariable Long id) {
        log.info("Get task bundle details for ID: {}", id);
        
        TaskBundleResponse response = taskBundleService.getTaskBundleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update task bundle
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> updateTaskBundle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskBundleRequest request) {
        log.info("Update task bundle request for ID: {}", id);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleResponse response = taskBundleService.updateTaskBundle(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete task bundle (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<Map<String, Object>> deleteTaskBundle(@PathVariable Long id) {
        log.info("Delete task bundle request for ID: {}", id);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        taskBundleService.deleteTaskBundle(id, currentUserId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Task bundle deleted successfully");
        response.put("bundleId", id);
        response.put("deleted", true);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Activate task bundle
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> activateTaskBundle(@PathVariable Long id) {
        log.info("Activate task bundle request for ID: {}", id);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleResponse response = taskBundleService.activateTaskBundle(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate task bundle
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> deactivateTaskBundle(@PathVariable Long id) {
        log.info("Deactivate task bundle request for ID: {}", id);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleResponse response = taskBundleService.deactivateTaskBundle(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get task bundles by role
     */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<List<TaskBundleListResponse>> getTaskBundlesByRole(@PathVariable Long roleId) {
        log.info("Get task bundles for role ID: {}", roleId);
        
        List<TaskBundleListResponse> bundles = taskBundleService.getTaskBundlesByRole(roleId);
        return ResponseEntity.ok(bundles);
    }

    /**
     * Add task to task bundle
     */
    @PostMapping("/{bundleId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleTaskResponse> addTaskToBundle(
            @PathVariable Long bundleId,
            @Valid @RequestBody CreateTaskBundleTaskRequest request) {
        log.info("Add task to bundle {} request: {}", bundleId, request.getTitle());
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleTaskResponse response = taskBundleService.addTaskToBundle(bundleId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get tasks in task bundle
     */
    @GetMapping("/{bundleId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleResponse> getTasksInBundle(@PathVariable Long bundleId) {
        log.info("Get tasks in bundle: {}", bundleId);

        TaskBundleResponse response = taskBundleService.getTasksInBundle(bundleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update task in task bundle
     */
    @PutMapping("/{bundleId}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskBundleTaskResponse> updateTaskInBundle(
            @PathVariable Long bundleId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskBundleTaskRequest request) {
        log.info("Update task {} in bundle {} request", taskId, bundleId);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskBundleTaskResponse response = taskBundleService.updateTaskInBundle(bundleId, taskId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove task from task bundle (soft delete)
     */
    @DeleteMapping("/{bundleId}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<Void> removeTaskFromBundle(
            @PathVariable Long bundleId,
            @PathVariable Long taskId) {
        log.info("Remove task {} from bundle {} request", taskId, bundleId);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        taskBundleService.removeTaskFromBundle(bundleId, taskId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually trigger bundle execution
     */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<String> triggerBundleExecution(@PathVariable Long id) {
        log.info("Manual trigger execution for bundle: {}", id);
        
        bundleSchedulerService.triggerBundleExecution(id);
        return ResponseEntity.ok("Bundle execution triggered successfully");
    }

    /**
     * Execute bundle now with detailed response
     */
    @PostMapping("/{id}/execute-now")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<BundleExecutionResponse> executeBundleNow(@PathVariable Long id) {
        log.info("Execute now request for bundle: {}", id);
        
        BundleSchedulerService.BundleExecutionResult result = bundleSchedulerService.executeBundleNow(id);
        return ResponseEntity.ok(result.getResponse());
    }

    /**
     * Manually generate tasks for a bundle on specific date
     */
    @PostMapping("/{id}/generate-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskGenerationService.GenerationResult> generateTasksForBundle(
            @PathVariable Long id,
            @RequestParam LocalDate executionDate) {
        log.info("Manual task generation for bundle {} on date: {}", id, executionDate);
        
        TaskGenerationService.GenerationResult result = taskGenerationService.manuallyGenerateTasks(id, executionDate);
        return ResponseEntity.ok(result);
    }

    /**
     * Get bundle execution statistics
     */
    @GetMapping("/{id}/execution-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<BundleSchedulerService.BundleExecutionStats> getBundleExecutionStats(@PathVariable Long id) {
        log.info("Get execution statistics for bundle: {}", id);
        
        BundleSchedulerService.BundleExecutionStats stats = bundleSchedulerService.getBundleExecutionStats(id);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get bundles scheduled for next hour
     */
    @GetMapping("/scheduled-next-hour")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<List<TaskBundleListResponse>> getBundlesScheduledNextHour() {
        log.info("Get bundles scheduled for next hour");
        
        List<com.lab.atlasmentor.model.TaskBundle> bundles = bundleSchedulerService.getBundlesScheduledNextHour();
        List<TaskBundleListResponse> response = bundles.stream()
                .map(bundle -> {
                    TaskBundleListResponse listResponse = new TaskBundleListResponse();
                    listResponse.setId(bundle.getId());
                    listResponse.setName(bundle.getName());
                    listResponse.setDescription(bundle.getDescription());
                    listResponse.setStatus(bundle.getStatus());
                    listResponse.setLastExecutedAt(bundle.getLastExecutedAt());
                    listResponse.setNextExecutionAt(bundle.getNextExecutionAt());
                    listResponse.setCreatedAt(bundle.getCreatedAt());
                    listResponse.setCreatedBy(bundle.getCreatedBy());
                    
                    if (bundle.getRole() != null) {
                        listResponse.setRoleName(bundle.getRole().getName());
                    }
                    
                    if (bundle.getSchedule() != null) {
                        listResponse.setScheduleType(bundle.getSchedule().getScheduleType().name());
                    }
                    
                    return listResponse;
                })
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get task generation statistics
     */
    @GetMapping("/generation-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskGenerationService.TaskGenerationStats> getTaskGenerationStats(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("Get task generation statistics from {} to {}", startDate, endDate);
        
        TaskGenerationService.TaskGenerationStats stats = taskGenerationService.getGenerationStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Process overdue tasks manually
     */
    @PostMapping("/process-overdue-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<OverdueTaskSchedulerService.OverdueTaskResult> processOverdueTasks() {
        log.info("Manual processing of overdue tasks");
        
        OverdueTaskSchedulerService.OverdueTaskResult result = overdueTaskSchedulerService.processOverdueTasksManually();
        return ResponseEntity.ok(result);
    }

    /**
     * Get overdue task statistics
     */
    @GetMapping("/overdue-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<OverdueTaskSchedulerService.OverdueTaskStats> getOverdueTaskStats() {
        log.info("Get overdue task statistics");
        
        OverdueTaskSchedulerService.OverdueTaskStats stats = overdueTaskSchedulerService.getOverdueTaskStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Generate tasks for specific user by role
     */
    @PostMapping("/generate-tasks/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<TaskGenerationService.GenerationResult> generateTasksForUser(
            @PathVariable Long userId,
            @RequestParam LocalDate executionDate) {
        log.info("Generate tasks for user {} on date: {}", userId, executionDate);
        
        TaskGenerationService.GenerationResult result = taskGenerationService.generateTasksForUserByRole(userId, executionDate);
        return ResponseEntity.ok(result);
    }

    /**
     * Get overdue tasks for user
     */
    @GetMapping("/overdue-tasks/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<List<com.lab.atlasmentor.dto.TaskResponse>> getOverdueTasksForUser(@PathVariable Long userId) {
        log.info("Get overdue tasks for user: {}", userId);
        
        List<com.lab.atlasmentor.model.Task> tasks = overdueTaskSchedulerService.getOverdueTasksForUser(userId);
        List<TaskResponse> response = tasks.stream()
                .map(task -> {
                    TaskResponse taskResponse = new TaskResponse();
                    taskResponse.setId(task.getId());
                    taskResponse.setTitle(task.getTitle());
                    taskResponse.setDescription(task.getDescription());
                    taskResponse.setStatus(task.getStatus());
                    taskResponse.setPriority(task.getPriority());
                    taskResponse.setDueDate(task.getDueDate());
                    taskResponse.setExecutionDate(task.getExecutionDate());
                    taskResponse.setSourceType(task.getSourceType());
                    taskResponse.setCreatedAt(task.getCreatedAt());
                    taskResponse.setUpdatedAt(task.getUpdatedAt());
                    taskResponse.setCreatedBy(task.getCreatedBy());
                    taskResponse.setUpdatedBy(task.getUpdatedBy());
                    
                    // Set assigned user
                    if (task.getAssignedTo() != null) {
                        taskResponse.setAssigneeName(task.getAssignedTo().getFullName());
                        taskResponse.setAssignedToId(task.getAssignedTo().getId());
                    }
                    
                    // Set assigned by user
                    if (task.getAssignedBy() != null) {
                        taskResponse.setAssignerName(task.getAssignedBy().getFullName());
                        taskResponse.setAssignedById(task.getAssignedBy().getId());
                    }
                    
                    // Set bundle info
                    if (task.getTaskBundle() != null) {
                        taskResponse.setTaskBundleId(task.getTaskBundle().getId());
                        taskResponse.setTaskBundleName(task.getTaskBundle().getName());
                    }
                    
                    // Set branch info
                    if (task.getBranch() != null) {
                        taskResponse.setBranchId(task.getBranch().getId());
                        taskResponse.setBranchName(task.getBranch().getName());
                    }
                    
                    return taskResponse;
                })
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get overdue tasks by branch
     */
    @GetMapping("/overdue-tasks/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<List<TaskResponse>> getOverdueTasksByBranch(@PathVariable Long branchId) {
        log.info("Get overdue tasks for branch: {}", branchId);
        
        List<com.lab.atlasmentor.model.Task> tasks = overdueTaskSchedulerService.getOverdueTasksByBranch(branchId);
        List<TaskResponse> response = tasks.stream()
                .map(task -> {
                    TaskResponse taskResponse = new TaskResponse();
                    taskResponse.setId(task.getId());
                    taskResponse.setTitle(task.getTitle());
                    taskResponse.setDescription(task.getDescription());
                    taskResponse.setStatus(task.getStatus());
                    taskResponse.setPriority(task.getPriority());
                    taskResponse.setDueDate(task.getDueDate());
                    taskResponse.setExecutionDate(task.getExecutionDate());
                    taskResponse.setSourceType(task.getSourceType());
                    taskResponse.setCreatedAt(task.getCreatedAt());
                    taskResponse.setUpdatedAt(task.getUpdatedAt());
                    taskResponse.setCreatedBy(task.getCreatedBy());
                    taskResponse.setUpdatedBy(task.getUpdatedBy());
                    
                    // Set assigned user
                    if (task.getAssignedTo() != null) {
                        taskResponse.setAssigneeName(task.getAssignedTo().getFullName());
                        taskResponse.setAssignedToId(task.getAssignedTo().getId());
                    }
                    
                    // Set assigned by user
                    if (task.getAssignedBy() != null) {
                        taskResponse.setAssignerName(task.getAssignedBy().getFullName());
                        taskResponse.setAssignedById(task.getAssignedBy().getId());
                    }
                    
                    // Set bundle info
                    if (task.getTaskBundle() != null) {
                        taskResponse.setTaskBundleId(task.getTaskBundle().getId());
                        taskResponse.setTaskBundleName(task.getTaskBundle().getName());
                    }
                    
                    // Set branch info
                    if (task.getBranch() != null) {
                        taskResponse.setBranchId(task.getBranch().getId());
                        taskResponse.setBranchName(task.getBranch().getName());
                    }
                    
                    return taskResponse;
                })
                .toList();
        
        return ResponseEntity.ok(response);
    }
}
