package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.service.TaskService;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    // SecurityUtil removed - now using SecurityUtils directly

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateTaskRequest request) {
        log.info("Create task request: {}", request.getTitle());
        
        try {
            // Extract current user using SecurityUtils
            Long currentUserId = SecurityUtils.getCurrentUserId();
            log.info("Current user ID extracted: {}", currentUserId);
            
            TaskResponse response = taskService.createTask(request, currentUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(required = false) String assignedDateFrom,
            @RequestParam(required = false) String assignedDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Get tasks with filters: status={}, assigneeId={}, branchId={}, priority={}, createdBy={}, keyword={}, overdue={}, search={}, dueDateFrom={}, dueDateTo={}, assignedDateFrom={}, assignedDateTo={}, page={}, size={}", 
                status, assigneeId, branchId, priority, createdBy, keyword, overdue, search, dueDateFrom, dueDateTo, assignedDateFrom, assignedDateTo, page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<TaskResponse> tasks = taskService.getTasksWithFiltersPaginated(status, assigneeId, branchId, priority, createdBy, keyword, overdue, search, dueDateFrom, dueDateTo, assignedDateFrom, assignedDateTo, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<TaskResponse>> getAllTasksPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Get paginated tasks: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<TaskResponse> taskPage = taskService.getAllTasksPaginated(pageable);
        return ResponseEntity.ok(taskPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskDetails(@PathVariable Long id) {
        log.info("Get task details for ID: {}", id);
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<CombinedTaskDetailsResponse> getCombinedTaskDetails(@PathVariable Long id) {
        log.info("Get combined task details for ID: {}", id);
        CombinedTaskDetailsResponse response = taskService.getCombinedTaskDetails(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        log.info("Update task status request for ID: {} to status: {}", id, request.getStatus());
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        TaskResponse response = taskService.updateTaskStatus(id, request.getStatus(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/assignee")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AssignTaskRequest request) {
        log.info("Assign task request for ID: {} to user: {}", id, request.getAssignedToId());
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        TaskResponse response = taskService.assignTask(id, request.getAssignedToId(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<TaskResponse> updateTaskPriority(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdatePriorityRequest request) {
        log.info("Update task priority request for ID: {} to priority: {}", id, request.getPriority());
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        TaskResponse response = taskService.updateTaskPriority(id, request.getPriority(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/due-date")
    public ResponseEntity<TaskResponse> updateTaskDueDate(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateDueDateRequest request) {
        log.info("Update task due date request for ID: {} to date: {}", id, request.getDueDate());
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        TaskResponse response = taskService.updateTaskDueDate(id, request.getDueDate(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TaskCommentResponse>> getTaskComments(@PathVariable Long id) {
        log.info("Get comments for task ID: {}", id);
        List<TaskCommentResponse> comments = taskService.getTaskComments(id);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskCommentResponse> addComment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AddCommentRequest request) {
        log.info("Add comment request for task ID: {}", id);
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        TaskCommentResponse response = taskService.addComment(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<ActivityResponse>> getTaskActivity(@PathVariable Long id) {
        log.info("Get task activity for ID: {}", id);
        List<ActivityResponse> activities = taskService.getTaskActivities(id);
        return ResponseEntity.ok(activities);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Delete task request for ID: {}", id);
        
        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        taskService.deleteTask(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getTaskStatuses() {
        log.info("Get all task statuses");
        List<Map<String, String>> statuses = Arrays.stream(TaskStatus.values())
                .map(status -> Map.of(
                        "value", status.name(),
                        "label", formatEnumLabel(status.name())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/priorities")
    public ResponseEntity<List<Map<String, String>>> getTaskPriorities() {
        log.info("Get all task priorities");
        List<Map<String, String>> priorities = Arrays.stream(Priority.values())
                .map(priority -> Map.of(
                        "value", priority.name(),
                        "label", formatEnumLabel(priority.name())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(priorities);
    }

    private String formatEnumLabel(String enumName) {
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
