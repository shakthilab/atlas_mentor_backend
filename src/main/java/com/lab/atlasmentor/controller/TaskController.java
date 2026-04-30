package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.service.TaskService;
import com.lab.atlasmentor.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateTaskRequest request) {
        log.info("Create task request: {}", request.getTitle());
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskResponse response = taskService.createTask(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean overdue) {
        log.info("Get tasks with filters: status={}, assigneeId={}, branchId={}, priority={}, createdBy={}, keyword={}, overdue={}", 
                status, assigneeId, branchId, priority, createdBy, keyword, overdue);
        
        List<TaskResponse> tasks = taskService.getTasksWithFilters(status, assigneeId, branchId, priority, createdBy, keyword, overdue);
        return ResponseEntity.ok(tasks);
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        log.info("Update task status request for ID: {} to status: {}", id, request.getStatus());
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskResponse response = taskService.updateTaskStatus(id, request.getStatus(), currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AssignTaskRequest request) {
        log.info("Assign task request for ID: {} to user: {}", id, request.getAssignedToId());
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskResponse response = taskService.assignTask(id, request.getAssignedToId(), currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<TaskResponse> updateTaskPriority(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdatePriorityRequest request) {
        log.info("Update task priority request for ID: {} to priority: {}", id, request.getPriority());
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskResponse response = taskService.updateTaskPriority(id, request.getPriority(), currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/due-date")
    public ResponseEntity<TaskResponse> updateTaskDueDate(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateDueDateRequest request) {
        log.info("Update task due date request for ID: {} to date: {}", id, request.getDueDate());
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskResponse response = taskService.updateTaskDueDate(id, request.getDueDate(), currentUser.getId());
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
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        TaskCommentResponse response = taskService.addComment(id, request, currentUser.getId());
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
        
        // Extract current user from token
        User currentUser = securityUtil.extractUserFromToken(token);
        
        taskService.deleteTask(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
