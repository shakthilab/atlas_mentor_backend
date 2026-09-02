package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.service.TaskService;
import com.lab.atlasmentor.service.TaskAttachmentUploadService;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final TaskAttachmentUploadService taskAttachmentUploadService;

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
        } catch (BusinessException e) {
            log.error("Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TaskPageWithStatsResponse>> getAllTasks(
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
        TaskStatsResponse stats = taskService.getTaskStats(status, assigneeId, branchId, priority, createdBy, keyword, overdue, search, dueDateFrom, dueDateTo, assignedDateFrom, assignedDateTo);
        TaskPageWithStatsResponse response = new TaskPageWithStatsResponse(stats, tasks);

        if (tasks.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", response));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getAllTasksPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Get paginated tasks: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<TaskResponse> taskPage = taskService.getAllTasksPaginated(pageable);
        if (taskPage.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", taskPage));
        }
        return ResponseEntity.ok(ApiResponse.success(taskPage));
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

    /**
     * Same behavior as PUT /{id}/status - added because Part D1 of the employee daily
     * workflow spec calls for PATCH semantics (a partial update of just the status field).
     * PUT is kept above for existing callers.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> patchTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        log.info("Patch task status request for ID: {} to status: {}", id, request.getStatus());

        Long currentUserId = SecurityUtils.getCurrentUserId();

        TaskResponse response = taskService.updateTaskStatus(id, request.getStatus(), currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Employee-triggered: only valid when the task is currently REFLECT. Restores the
     * pre-flag status and routes re-review back to whichever stage flagged it (see
     * TaskService#resubmitTask / DayApprovalService).
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<TaskResponse> resubmitTask(@PathVariable Long id) {
        log.info("Resubmit request for task ID: {}", id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.resubmitTask(id, currentUserId);
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

    @PutMapping("/{id}/due-time")
    public ResponseEntity<TaskResponse> updateTaskDueTime(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateDueTimeRequest request) {
        log.info("Update task due time request for ID: {} to time: {}", id, request.getDueTime());

        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();

        TaskResponse response = taskService.updateTaskDueTime(id, request.getDueTime(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getTaskComments(@PathVariable Long id) {
        log.info("Get comments for task ID: {}", id);
        List<TaskCommentResponse> comments = taskService.getTaskComments(id);
        if (comments.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", comments));
        }
        return ResponseEntity.ok(ApiResponse.success(comments));
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

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateCommentRequest request) {
        log.info("Update comment {} request for task ID: {}", commentId, id);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        TaskCommentResponse response = taskService.updateComment(id, commentId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token) {
        log.info("Delete comment {} request for task ID: {}", commentId, id);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        taskService.deleteComment(id, commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> getTaskAttachments(@PathVariable Long id) {
        log.info("Get attachments for task ID: {}", id);
        List<TaskAttachmentResponse> attachments = taskService.getTaskAttachments(id);
        if (attachments.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", attachments));
        }
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<TaskAttachmentResponse> addAttachment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AddAttachmentRequest request) {
        log.info("Add attachment request for task ID: {}", id);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        TaskAttachmentResponse response = taskService.addAttachment(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * The shared upload endpoint for both attachment contexts (see
     * TaskAttachmentUploadService's javadoc): the proof section calls this with
     * commentId omitted, the comment section calls it with commentId set to the
     * comment the file is attached to. Same file, same compression, same R2 upload
     * either way - the two contexts differ only in what commentId is sent.
     */
    @PostMapping("/{id}/attachments/upload")
    public ResponseEntity<TaskAttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "commentId", required = false) Long commentId) {
        log.info("Upload attachment request for task ID: {}, commentId: {}", id, commentId);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        TaskAttachmentResponse response = taskAttachmentUploadService.uploadAttachment(id, file, commentId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getTaskActivity(@PathVariable Long id) {
        log.info("Get task activity for ID: {}", id);
        List<ActivityResponse> activities = taskService.getTaskActivities(id);
        if (activities.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", activities));
        }
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        log.info("Delete task request for ID: {}", id);

        // Extract current user using SecurityUtils
        Long currentUserId = SecurityUtils.getCurrentUserId();

        taskService.deleteTask(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<BulkDeleteTaskResponse>> bulkDeleteTasks(
            @Valid @RequestBody BulkDeleteTaskRequest request) {
        log.info("REST request to bulk delete {} task(s)", request.getTaskIds().size());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BulkDeleteTaskResponse response = taskService.deleteTasks(request.getTaskIds(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success(
                response.getDeletedCount() + " task(s) deleted successfully", response));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<BulkDeleteTaskResponse>> bulkDeleteTasksViaDelete(
            @Valid @RequestBody BulkDeleteTaskRequest request) {
        log.info("REST request via DELETE to bulk delete {} task(s)", request.getTaskIds().size());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BulkDeleteTaskResponse response = taskService.deleteTasks(request.getTaskIds(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success(
                response.getDeletedCount() + " task(s) deleted successfully", response));
    }

    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getTaskStatuses() {
        log.info("Get all task statuses");
        // Deliberately not every TaskStatus value - this feeds the UI's status filter/picker,
        // and OVERDUE/REFLECT/VERIFIED/CANCELLED/REVIEW are workflow-driven states the
        // employee shouldn't be picking directly (see TaskService#validateStatusTransition).
        // DONE, not COMPLETED - DONE is the value the actual TODO -> IN_PROGRESS -> DONE
        // production flow ends on; COMPLETED is the legacy manual-flow synonym for it
        // (see TaskService#validateStatusTransition's javadoc). The API itself still
        // accepts/returns every status elsewhere; this list is just the selectable subset.
        List<TaskStatus> selectableStatuses = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
        List<Map<String, String>> statuses = selectableStatuses.stream()
                .map(status -> Map.of(
                        "value", status.name(),
                        "label", formatEnumLabel(status.name())
                ))
                .collect(Collectors.toList());
        if (statuses.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", statuses));
        }
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getTaskPriorities() {
        log.info("Get all task priorities");
        List<Map<String, String>> priorities = Arrays.stream(Priority.values())
                .map(priority -> Map.of(
                        "value", priority.name(),
                        "label", formatEnumLabel(priority.name())
                ))
                .collect(Collectors.toList());
        if (priorities.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", priorities));
        }
        return ResponseEntity.ok(ApiResponse.success(priorities));
    }

    private String formatEnumLabel(String enumName) {
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
