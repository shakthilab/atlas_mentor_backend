package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.security.AccessScopeService;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final TaskDisplayIdService taskDisplayIdService;
    private final AccessScopeService accessScopeService;

    public TaskResponse createTask(CreateTaskRequest request, Long createdByUserId) {
        log.info("Creating new task with title: {}", request.getTitle());

        User assignedTo = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new RuntimeException("Assignee user not found with ID: " + request.getAssignedToId()));

        // Validate that the creator user exists, provide fallback if needed
        User createdBy = userRepository.findById(createdByUserId).orElse(null);
        if (createdBy == null) {
            log.warn("Creator user not found with ID: {}. The user may have been deleted or the JWT token contains an invalid user ID. Using fallback user.", createdByUserId);
            // Use the first available admin user as fallback
            createdBy = userRepository.findFirstByRoleName("ADMIN").orElseThrow(() -> 
                new RuntimeException("No admin users found in the system to use as fallback"));
            log.info("Using fallback admin user ID: {}", createdBy.getId());
        }

        // Validate task creation permissions based on roles
        validateTaskCreation(createdBy, assignedTo);

        // Auto-assign branch from assigned user
        Branch branch = assignedTo.getBranch();
        
        // If admin is assigning to another admin, keep branch null (admin sees all tasks)
        if (branch == null && !createdBy.hasRole("ADMIN")) {
            // For non-admin assigners, ensure task has a branch
            log.warn("Task assigned to user ID {} who has no branch assignment", assignedTo.getId());
        }
        
        log.info("Task creation: assigned_to_id={}, branch_id={}, created_by_id={}",
            assignedTo.getId(), branch != null ? branch.getId() : "null", createdBy.getId());

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                assignedTo,
                createdBy, // assignedBy
                createdBy, // createdBy
                request.getPriority() != null ? request.getPriority() : Priority.MEDIUM,
                request.getDueDate(),
                branch
        );

        task.setReferenceType(request.getReferenceType());
        task.setReferenceId(request.getReferenceId());
        task.setDueTime(request.getDueTime());
        task.setDisplayId(taskDisplayIdService.nextDisplayId(assignedTo.getRole()));
        task.setCreatedBy(createdBy.getId());
        task.setUpdatedBy(createdBy.getId());
        // current_step/next_step (V18) - null/null here since this manual-creation path never
        // attaches a day workspace; see DayApprovalService#applyStepLabels.
        DayApprovalService.applyStepLabels(task);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.CREATED,
                null,
                null,
                createdBy
        );
        activity.setCreatedBy(createdBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task created successfully with ID: {}", savedTask.getId());
        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTaskStatus(Long taskId, TaskStatus newStatus, Long updatedByUserId) {
        log.info("Updating task {} status to {}", taskId, newStatus);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate update permissions based on roles
        validateTaskUpdate(updatedBy, task);

        // Validate status transition
        TaskStatus oldStatus = task.getStatus();
        String userRole = updatedBy.getRole() != null ? updatedBy.getRole().getName() : null;
        validateStatusTransition(oldStatus, newStatus, userRole);

        // Leaving REFLECT via this generic path (normally only possible for ADMIN, which
        // bypasses validateStatusTransition's REFLECT -> IN_PROGRESS-only rule above) must
        // clear the reflect-cycle fields alongside it. Otherwise reflect_state/reflect_stage
        // are left stamped from the old cycle - status reads DONE everywhere, but
        // DayApprovalService#approveResubmittedTasks still sees reflect_state = FLAGGED and
        // rejects it, with no way for a reviewer to tell why from the task's visible status.
        // resubmitTask() is the only other path off REFLECT and manages these fields itself.
        if (oldStatus == TaskStatus.REFLECT && newStatus != TaskStatus.REFLECT) {
            task.setReflectState(null);
            task.setReflectStage(null);
            task.setReflectComment(null);
            task.setReflectFlaggedAt(null);
            task.setReflectFlaggedBy(null);
            task.setReflectResubmittedAt(null);
            task.setReflectPreviousStatus(null);
            // Reflect cycle just closed via this (ADMIN-only) path too - recompute
            // current_step/next_step from the day's own approval_stage, same as the
            // approval-workflow path (DayApprovalService#approveResubmittedTasks).
            DayApprovalService.applyStepLabels(task);
        }

        task.setStatus(newStatus);
        task.setUpdatedBy(updatedBy.getId());

        // completed_at (V23, Overdue Task Rollover spec): a direct, queryable "when was this
        // actually done" - independent of due_date, which never moves. Stamped the moment
        // status first becomes DONE/COMPLETED, however many days past due_date that is;
        // cleared if an ADMIN override later moves it back off a completed state (the only
        // way that's reachable - validateStatusTransition blocks it for everyone else).
        boolean wasDone = oldStatus == TaskStatus.DONE || oldStatus == TaskStatus.COMPLETED;
        boolean isDone = newStatus == TaskStatus.DONE || newStatus == TaskStatus.COMPLETED;
        if (isDone && !wasDone) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (wasDone && !isDone) {
            task.setCompletedAt(null);
        }

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.STATUS_CHANGED,
                oldStatus.toString(),
                newStatus.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task {} status updated successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    /**
     * Employee resubmits a REFLECT task once they've fixed it (Part 5 of the send-back /
     * rework spec). Restores whatever status the task had immediately before it was
     * flagged, and marks it RESUBMITTED against the same stage that flagged it (Part 4 -
     * Option A: routes back to that one stage, not a restart of Partner -> Manager ->
     * Admin) so it surfaces in that stage's re-review queue (GET /api/approvals/pending).
     * Touches only this one task - no other task or the day's own approval_stage is
     * affected.
     */
    public TaskResponse resubmitTask(Long taskId, Long currentUserId) {
        log.info("Resubmitting task {} by user {}", taskId, currentUserId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus() != TaskStatus.REFLECT) {
            throw new BusinessException("Task " + taskId + " is not currently in REFLECT status (current: " + task.getStatus() + ")");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long assignedToId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole() != null ? currentUser.getRole().getName() : null);
        if (!isAdmin && !currentUserId.equals(assignedToId)) {
            throw new UnauthorizedAccessException("Only the assignee can resubmit their own task");
        }

        TaskStatus restoredStatus = parseTaskStatusOrDefault(task.getReflectPreviousStatus(), TaskStatus.IN_PROGRESS);
        String owningStage = task.getReflectStage();

        // completed_at (V23): the employee is re-claiming "done" right now, having just fixed
        // what the reviewer flagged - restoredStatus can legitimately be DONE/COMPLETED
        // (sendBackTasks captures whatever status the task was in when flagged). Mirrors
        // updateTaskStatus's own stamping of this column.
        if (restoredStatus == TaskStatus.DONE || restoredStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        task.setStatus(restoredStatus);
        task.setReflectState("RESUBMITTED");
        task.setReflectResubmittedAt(LocalDateTime.now());
        task.setReflectPreviousStatus(null);
        // reflectStage / reflectComment / reflectFlaggedAt / reflectFlaggedBy are kept as-is
        // so the reviewing stage still has the original send-back context when they re-check it.
        task.setUpdatedBy(currentUserId);
        DayApprovalService.applyStepLabels(task);
        Task savedTask = taskRepository.save(task);

        String stageLabel = owningStage != null ? owningStage.replace('_', ' ') : "the reviewer";
        TaskActivity activity = new TaskActivity(savedTask, TaskAction.STATUS_CHANGED,
                TaskStatus.REFLECT.name(), restoredStatus.name(), currentUser,
                "Resubmitted for " + stageLabel + " re-review");
        activity.setCreatedBy(currentUserId);
        taskActivityRepository.save(activity);

        log.info("Task {} resubmitted - restored to {}, awaiting {} re-review", taskId, restoredStatus, owningStage);
        return convertToTaskResponse(savedTask);
    }

    private TaskStatus parseTaskStatusOrDefault(String value, TaskStatus fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return TaskStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse stored reflectPreviousStatus '{}', defaulting to {}", value, fallback);
            return fallback;
        }
    }

    public TaskResponse assignTask(Long taskId, Long assignedToUserId, Long assignedByUserId) {
        log.info("Assigning task {} to user {}", taskId, assignedToUserId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User newAssignee = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("Assignee user not found"));

        User assignedBy = userRepository.findById(assignedByUserId)
                .orElseThrow(() -> new RuntimeException("Assigner user not found"));

        // Validate update permissions based on roles (for modifying the task)
        validateTaskUpdate(assignedBy, task);
        
        // Validate assignment permissions based on roles (for who can be assigned)
        validateTaskAssignment(assignedBy, newAssignee);

        String oldAssigneeName = task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null;
        String newAssigneeName = newAssignee.getFullName();

        task.setAssignedTo(newAssignee);
        task.setUpdatedBy(assignedBy.getId());

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.ASSIGNED,
                oldAssigneeName,
                newAssigneeName,
                assignedBy
        );
        activity.setCreatedBy(assignedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task {} assigned successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    public TaskCommentResponse addComment(Long taskId, AddCommentRequest request, Long commentedByUserId) {
        log.info("Adding comment to task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User commentedBy = userRepository.findById(commentedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TaskComment comment = new TaskComment(task, request.getComment(), commentedBy);
        if (request.getParentCommentId() != null) {
            TaskComment parent = taskCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (!parent.getTask().getId().equals(taskId)) {
                throw new BusinessException("Parent comment does not belong to this task");
            }
            comment.setParentComment(parent);
        }
        comment.setCreatedBy(commentedBy.getId());
        comment.setUpdatedBy(commentedBy.getId());

        TaskComment savedComment = taskCommentRepository.save(comment);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                task,
                TaskAction.COMMENT_ADDED,
                null,
                null,
                commentedBy
        );
        activity.setCreatedBy(commentedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Comment added successfully to task {}", taskId);
        return convertToTaskCommentResponse(savedComment);
    }

    public TaskCommentResponse updateComment(Long taskId, Long commentId, UpdateCommentRequest request, Long updatedByUserId) {
        log.info("Updating comment {} on task {}", commentId, taskId);

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (comment.getTask() == null || !comment.getTask().getId().equals(taskId)) {
            throw new BusinessException("Comment does not belong to this task");
        }

        if (comment.getCommentedBy() == null || !comment.getCommentedBy().getId().equals(updatedByUserId)) {
            throw new BusinessException("You can only edit your own comments");
        }

        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        comment.setComment(request.getComment());
        comment.setEdited(true);
        comment.setUpdatedBy(updatedByUserId);
        TaskComment savedComment = taskCommentRepository.save(comment);

        TaskActivity activity = new TaskActivity(
                comment.getTask(),
                TaskAction.COMMENT_EDITED,
                null,
                null,
                updatedBy
        );
        activity.setCreatedBy(updatedByUserId);
        taskActivityRepository.save(activity);

        log.info("Comment {} updated successfully on task {}", commentId, taskId);
        return convertToTaskCommentResponse(savedComment);
    }

    public TaskResponse updateTaskPriority(Long taskId, Priority newPriority, Long updatedByUserId) {
        log.info("Updating task {} priority to {}", taskId, newPriority);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate update permissions based on roles
        validateTaskUpdate(updatedBy, task);

        Priority oldPriority = task.getPriority();
        task.setPriority(newPriority);
        task.setUpdatedBy(updatedBy.getId());

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.PRIORITY_CHANGED,
                oldPriority.toString(),
                newPriority.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task {} priority updated successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTaskDueDate(Long taskId, LocalDate newDueDate, Long updatedByUserId) {
        log.info("Updating task {} due date to {}", taskId, newDueDate);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate update permissions based on roles
        validateTaskUpdate(updatedBy, task);

        LocalDate oldDueDate = task.getDueDate();
        task.setDueDate(newDueDate);
        task.setUpdatedBy(updatedBy.getId());

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.DUE_DATE_UPDATED,
                oldDueDate != null ? oldDueDate.toString() : null,
                newDueDate.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task {} due date updated successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTaskDueTime(Long taskId, LocalDateTime newDueTime, Long updatedByUserId) {
        log.info("Updating task {} due time to {}", taskId, newDueTime);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate update permissions based on roles
        validateTaskUpdate(updatedBy, task);

        LocalDateTime oldDueTime = task.getDueTime();
        task.setDueTime(newDueTime);
        task.setUpdatedBy(updatedBy.getId());

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.DUE_TIME_UPDATED,
                oldDueTime != null ? oldDueTime.toString() : null,
                newDueTime != null ? newDueTime.toString() : null,
                updatedBy
        );
        activity.setCreatedBy(updatedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Task {} due time updated successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    public void deleteTask(Long taskId, Long deletedByUserId) {
        log.info("Soft deleting task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User deletedBy = userRepository.findById(deletedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate update permissions based on roles (deleting is a form of updating)
        validateTaskUpdate(deletedBy, task);

        task.setIsDeleted(true);
        task.setUpdatedBy(deletedBy.getId());

        taskRepository.save(task);

        log.info("Task {} soft deleted successfully", taskId);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        log.info("Getting all tasks with role-based access control");
        
        try {
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Current user: userId={}, role={}, branchId={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId());
            
            String userRole = currentUser.getRole();
            Long userId = currentUser.getUserId();
            Long branchId = currentUser.getBranchId();
            
            List<Task> tasks;
            
            switch (userRole.toUpperCase()) {
                case "ADMIN":
                    // ADMIN: return all tasks
                    tasks = taskRepository.findAllTasksForAdminList();
                    break;
                    
                case "MANAGER":
                case "ADMINISTRATIVE_ASSISTANT":
                    // MANAGER: return tasks WHERE branchId = user.branchId
                    tasks = taskRepository.findTasksByBranchForManagerList(branchId);
                    break;
                    
                case "SENIOR_COUNSELLOR":
                    // SENIOR_COUNSELLOR: return tasks WHERE branchId = user.branchId AND (assignedTo = user.id OR assignedTo IN juniorIds)
                    List<Long> juniorIds = getJuniorCounsellorIds(userId, branchId);
                    tasks = taskRepository.findTasksForSeniorCounsellorList(branchId, userId, juniorIds);
                    break;
                    
                case "JUNIOR_COUNSELLOR":
                    // JUNIOR_COUNSELLOR: return tasks WHERE branchId = user.branchId AND assignedTo = user.id
                    tasks = taskRepository.findTasksForJuniorCounsellorList(branchId, userId);
                    break;
                    
                default:
                    // Other roles cannot view tasks
                    log.warn("User role {} is not authorized to view tasks", userRole);
                    return List.of();
            }
            
            return tasks.stream()
                    .map(this::convertToTaskResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            throw new RuntimeException("SecurityUtils error: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasksPaginated(Pageable pageable) {
        log.info("Getting all tasks with role-based access control and pagination");
        
        try {
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Current user: userId={}, role={}, branchId={}, page={}, size={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), 
                pageable.getPageNumber(), pageable.getPageSize());
            
            String userRole = currentUser.getRole();
            Long userId = currentUser.getUserId();
            Long branchId = currentUser.getBranchId();
            
            Page<Task> taskPage;
            
            switch (userRole.toUpperCase()) {
                case "ADMIN":
                    // ADMIN: return all tasks
                    taskPage = taskRepository.findAllTasksForAdmin(pageable);
                    break;
                    
                case "MANAGER":
                case "ADMINISTRATIVE_ASSISTANT":
                    // MANAGER: return tasks WHERE branchId = user.branchId
                    taskPage = taskRepository.findTasksByBranchForManager(branchId, pageable);
                    break;
                    
                case "SENIOR_COUNSELLOR":
                    // SENIOR_COUNSELLOR: return tasks WHERE branchId = user.branchId AND (assignedTo = user.id OR assignedTo IN juniorIds)
                    List<Long> juniorIds = getJuniorCounsellorIds(userId, branchId);
                    taskPage = taskRepository.findTasksForSeniorCounsellor(branchId, userId, juniorIds, pageable);
                    break;
                    
                case "JUNIOR_COUNSELLOR":
                    // JUNIOR_COUNSELLOR: return tasks WHERE branchId = user.branchId AND assignedTo = user.id
                    taskPage = taskRepository.findTasksForJuniorCounsellor(branchId, userId, pageable);
                    break;
                    
                default:
                    // Other roles cannot view tasks
                    log.warn("User role {} is not authorized to view tasks", userRole);
                    return Page.empty(pageable);
            }
            
            return taskPage.map(this::convertToTaskResponse);
                    
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            throw new RuntimeException("SecurityUtils error: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksWithFilters(TaskStatus status, Long assigneeId, Long branchId,
                                                  Priority priority, Long createdBy, String keyword, Boolean overdue,
                                                  String search, String dueDateFrom, String dueDateTo,
                                                  String assignedDateFrom, String assignedDateTo) {
        log.info("Getting tasks with filters: status={}, assigneeId={}, branchId={}, priority={}, createdBy={}, keyword={}, overdue={}, search={}, dueDateFrom={}, dueDateTo={}, assignedDateFrom={}, assignedDateTo={}",
                status, assigneeId, branchId, priority, createdBy, keyword, overdue, search, dueDateFrom, dueDateTo, assignedDateFrom, assignedDateTo);

        // Get current user for branch-based filtering
        var currentUser = SecurityUtils.getCurrentUser();
        log.info("Current user: userId={}, role={}, branchId={}, isAdmin={}", 
            currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());

        final LocalDate dueDateFromParsed;
        final LocalDate dueDateToParsed;
        final LocalDate assignedDateFromParsed;
        final LocalDate assignedDateToParsed;

        try {
            dueDateFromParsed = parseDate(dueDateFrom);
            dueDateToParsed = parseDate(dueDateTo);
            assignedDateFromParsed = parseDate(assignedDateFrom);
            assignedDateToParsed = parseDate(assignedDateTo);
        } catch (Exception e) {
            log.error("Error parsing date filters: {}", e.getMessage());
            return List.of();
        }

        // Use role-based filtering (use status-aware methods when status is provided)
        List<Task> tasks;
        if (currentUser.isAdmin()) {
            // ADMIN: See all tasks
            if (status != null) {
                tasks = taskRepository.findByStatus(status);
            } else {
                tasks = taskRepository.findAllTasksForAdminList();
            }
        } else if ("MANAGER".equals(currentUser.getRole()) || "BRANCH_PARTNER".equals(currentUser.getRole()) || "ADMINISTRATIVE_ASSISTANT".equals(currentUser.getRole())) {
            // MANAGER/BRANCH_PARTNER: See all tasks in their branch
            if (status != null) {
                tasks = taskRepository.findByBranchIdAndStatus(currentUser.getBranchId(), status);
            } else {
                tasks = taskRepository.findTasksByBranchForManagerList(currentUser.getBranchId());
            }
        } else if ("SENIOR_COUNSELLOR".equals(currentUser.getRole())) {
            // SENIOR_COUNSELLOR: See their tasks + tasks of their junior counsellors
            // For now, fall back to branch filtering - TODO: Implement junior counsellor hierarchy
            if (status != null) {
                tasks = taskRepository.findByBranchIdAndStatus(currentUser.getBranchId(), status);
            } else {
                tasks = taskRepository.findTasksByBranchForManagerList(currentUser.getBranchId());
            }
        } else if ("JUNIOR_COUNSELLOR".equals(currentUser.getRole())) {
            // JUNIOR_COUNSELLOR: See only their assigned tasks
            if (status != null) {
                tasks = taskRepository.findByAssignedToIdAndStatus(currentUser.getUserId(), status);
            } else {
                tasks = taskRepository.findTasksForJuniorCounsellorList(currentUser.getBranchId(), currentUser.getUserId());
            }
        } else {
            // Default: Use the old access control method for other roles
            tasks = taskRepository.findAllWithAccess(currentUser.isAdmin(), currentUser.getBranchId(), currentUser.getUserId());
        }

        log.info("Total tasks fetched from DB before filtering: {}", tasks.size());
        if (status != null) {
            long matchingStatus = tasks.stream().filter(t -> t.getStatus() == status).count();
            log.info("Tasks with status {}: {} out of {}", status, matchingStatus, tasks.size());
        }

        List<TaskResponse> result = tasks.stream()
                .filter(task -> {
                    // Status filtering
                    if (status != null && task.getStatus() != status) {
                        log.debug("Filtering out task {} - status mismatch: expected={}, actual={}",
                                task.getId(), status, task.getStatus());
                        return false;
                    }

                    // Assignee filtering
                    if (assigneeId != null && (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(assigneeId))) {
                        return false;
                    }

                    // Branch filtering (for ADMIN who can see all branches)
                    if (branchId != null && (task.getBranch() == null || !task.getBranch().getId().equals(branchId))) {
                        return false;
                    }

                    // Priority filtering
                    if (priority != null && task.getPriority() != priority) {
                        return false;
                    }

                    // CreatedBy filtering
                    if (createdBy != null && (task.getCreatedBy() == null || !task.getCreatedBy().equals(createdBy))) {
                        return false;
                    }

                    // Overdue filtering
                    if (overdue != null && overdue) {
                        if (task.getDueDate() == null || task.getDueDate().isAfter(LocalDate.now()) || task.getStatus() == TaskStatus.COMPLETED) {
                            return false;
                        }
                    }

                    // Keyword/Search filtering (case-insensitive)
                    if (keyword != null && !keyword.isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean matchesKeyword = (task.getTitle() != null && task.getTitle().toLowerCase().contains(lowerKeyword)) ||
                                (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerKeyword));
                        if (!matchesKeyword) {
                            return false;
                        }
                    }

                    if (search != null && !search.isEmpty()) {
                        String lowerSearch = search.toLowerCase();
                        boolean matchesSearch = (task.getTitle() != null && task.getTitle().toLowerCase().contains(lowerSearch)) ||
                                (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerSearch));
                        if (!matchesSearch) {
                            return false;
                        }
                    }

                    // Due date filtering
                    if (dueDateFromParsed != null) {
                        if (task.getDueDate() == null || task.getDueDate().isBefore(dueDateFromParsed)) {
                            return false;
                        }
                    }
                    if (dueDateToParsed != null) {
                        if (task.getDueDate() == null || task.getDueDate().isAfter(dueDateToParsed)) {
                            return false;
                        }
                    }

                    // Assigned date filtering (createdAt)
                    LocalDate taskCreatedDate = task.getCreatedAt().toLocalDate();
                    if (assignedDateFromParsed != null && taskCreatedDate.isBefore(assignedDateFromParsed)) {
                        return false;
                    }
                    if (assignedDateToParsed != null && taskCreatedDate.isAfter(assignedDateToParsed)) {
                        return false;
                    }

                    return true;
                })
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());

        log.info("Tasks returned after filtering: {}", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksWithFiltersPaginated(TaskStatus status, Long assigneeId, Long branchId,
                                                             Priority priority, Long createdBy, String keyword, Boolean overdue,
                                                             String search, String dueDateFrom, String dueDateTo,
                                                             String assignedDateFrom, String assignedDateTo, Pageable pageable) {
        log.info("Getting tasks with filters (paginated): status={}, assigneeId={}, branchId={}, priority={}, createdBy={}, keyword={}, overdue={}, search={}, dueDateFrom={}, dueDateTo={}, assignedDateFrom={}, assignedDateTo={}, page={}, size={}",
                status, assigneeId, branchId, priority, createdBy, keyword, overdue, search, dueDateFrom, dueDateTo, assignedDateFrom, assignedDateTo, pageable.getPageNumber(), pageable.getPageSize());

        // Get current user for branch-based filtering
        var currentUser = SecurityUtils.getCurrentUser();
        log.info("Current user: userId={}, role={}, branchId={}, isAdmin={}",
            currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());

        final LocalDate dueDateFromParsed;
        final LocalDate dueDateToParsed;
        final LocalDate assignedDateFromParsed;
        final LocalDate assignedDateToParsed;

        try {
            dueDateFromParsed = parseDate(dueDateFrom);
            dueDateToParsed = parseDate(dueDateTo);
            assignedDateFromParsed = parseDate(assignedDateFrom);
            assignedDateToParsed = parseDate(assignedDateTo);
        } catch (Exception e) {
            log.error("Error parsing date filters: {}", e.getMessage());
            return Page.empty(pageable);
        }

        // Use role-based filtering with pagination (use status-aware methods when status is provided)
        Page<Task> taskPage;
        if (currentUser.isAdmin()) {
            // ADMIN: See all tasks
            if (status != null) {
                taskPage = taskRepository.findAllTasksForAdminWithStatus(status, pageable);
            } else {
                taskPage = taskRepository.findAllTasksForAdmin(pageable);
            }
        } else if ("MANAGER".equals(currentUser.getRole()) || "BRANCH_PARTNER".equals(currentUser.getRole()) || "ADMINISTRATIVE_ASSISTANT".equals(currentUser.getRole())) {
            // MANAGER/BRANCH_PARTNER: See all tasks in their branch
            if (status != null) {
                taskPage = taskRepository.findTasksByBranchForManagerWithStatus(currentUser.getBranchId(), status, pageable);
            } else {
                taskPage = taskRepository.findTasksByBranchForManager(currentUser.getBranchId(), pageable);
            }
        } else if ("SENIOR_COUNSELLOR".equals(currentUser.getRole())) {
            // SENIOR_COUNSELLOR: See their tasks + tasks of their junior counsellors
            List<Long> juniorIds = getJuniorCounsellorIds(currentUser.getUserId(), currentUser.getBranchId());
            if (status != null) {
                taskPage = taskRepository.findTasksForSeniorCounsellorWithStatus(currentUser.getBranchId(), currentUser.getUserId(), juniorIds, status.name(), pageable);
            } else {
                taskPage = taskRepository.findTasksForSeniorCounsellor(currentUser.getBranchId(), currentUser.getUserId(), juniorIds, pageable);
            }
        } else if ("JUNIOR_COUNSELLOR".equals(currentUser.getRole())) {
            // JUNIOR_COUNSELLOR: See only their assigned tasks
            if (status != null) {
                taskPage = taskRepository.findTasksForJuniorCounsellorWithStatus(currentUser.getBranchId(), currentUser.getUserId(), status, pageable);
            } else {
                taskPage = taskRepository.findTasksForJuniorCounsellor(currentUser.getBranchId(), currentUser.getUserId(), pageable);
            }
        } else {
            // Default: fetch all and filter (fallback)
            List<Task> allTasks = taskRepository.findAllWithAccess(currentUser.isAdmin(), currentUser.getBranchId(), currentUser.getUserId());
            // Apply filters and convert to page
            List<TaskResponse> filtered = applyFiltersAndConvert(allTasks, status, assigneeId, branchId, priority, createdBy,
                    keyword, overdue, search, dueDateFromParsed, dueDateToParsed, assignedDateFromParsed, assignedDateToParsed);
            return toPage(filtered, pageable);
        }

        log.info("Total tasks fetched from DB before filtering: {}", taskPage.getTotalElements());

        // Apply filters to the page content
        List<TaskResponse> filteredContent = applyFiltersAndConvert(taskPage.getContent(), status, assigneeId, branchId, priority, createdBy,
                keyword, overdue, search, dueDateFromParsed, dueDateToParsed, assignedDateFromParsed, assignedDateToParsed);

        // Create a new page with filtered content
        // Note: Since we filter after fetching, total count may not be accurate for filtered results
        // For accurate pagination with dynamic filters, we'd need a more complex query-based approach
        return new org.springframework.data.domain.PageImpl<>(filteredContent, pageable, taskPage.getTotalElements());
    }

    private List<Task> filterTasks(List<Task> tasks, TaskStatus status, Long assigneeId, Long branchId,
                                    Priority priority, Long createdBy, String keyword, Boolean overdue,
                                    String search, LocalDate dueDateFromParsed, LocalDate dueDateToParsed,
                                    LocalDate assignedDateFromParsed, LocalDate assignedDateToParsed) {
        return tasks.stream()
                .filter(task -> {
                    if (status != null && task.getStatus() != status) return false;
                    if (assigneeId != null && (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(assigneeId))) return false;
                    if (branchId != null && (task.getBranch() == null || !task.getBranch().getId().equals(branchId))) return false;
                    if (priority != null && task.getPriority() != priority) return false;
                    if (createdBy != null && (task.getCreatedBy() == null || !task.getCreatedBy().equals(createdBy))) return false;
                    if (overdue != null && overdue) {
                        if (task.getDueDate() == null || task.getDueDate().isAfter(LocalDate.now()) || task.getStatus() == TaskStatus.COMPLETED) return false;
                    }
                    if (keyword != null && !keyword.isEmpty()) {
                        String lk = keyword.toLowerCase();
                        if (!((task.getTitle() != null && task.getTitle().toLowerCase().contains(lk)) ||
                              (task.getDescription() != null && task.getDescription().toLowerCase().contains(lk)))) return false;
                    }
                    if (search != null && !search.isEmpty()) {
                        String ls = search.toLowerCase();
                        if (!((task.getTitle() != null && task.getTitle().toLowerCase().contains(ls)) ||
                              (task.getDescription() != null && task.getDescription().toLowerCase().contains(ls)))) return false;
                    }
                    if (dueDateFromParsed != null && (task.getDueDate() == null || task.getDueDate().isBefore(dueDateFromParsed))) return false;
                    if (dueDateToParsed != null && (task.getDueDate() == null || task.getDueDate().isAfter(dueDateToParsed))) return false;
                    LocalDate taskCreatedDate = task.getCreatedAt().toLocalDate();
                    if (assignedDateFromParsed != null && taskCreatedDate.isBefore(assignedDateFromParsed)) return false;
                    if (assignedDateToParsed != null && taskCreatedDate.isAfter(assignedDateToParsed)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<TaskResponse> applyFiltersAndConvert(List<Task> tasks, TaskStatus status, Long assigneeId, Long branchId,
                                                       Priority priority, Long createdBy, String keyword, Boolean overdue,
                                                       String search, LocalDate dueDateFromParsed, LocalDate dueDateToParsed,
                                                       LocalDate assignedDateFromParsed, LocalDate assignedDateToParsed) {
        return filterTasks(tasks, status, assigneeId, branchId, priority, createdBy, keyword, overdue,
                search, dueDateFromParsed, dueDateToParsed, assignedDateFromParsed, assignedDateToParsed)
                .stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    private Page<TaskResponse> toPage(List<TaskResponse> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) {
            start = list.size();
        }
        if (end > list.size()) {
            end = list.size();
        }
        List<TaskResponse> pageContent = list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDate.parse(dateStr);
    }

    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStats(TaskStatus status, Long assigneeId, Long branchId,
                                          Priority priority, Long createdBy, String keyword, Boolean overdue,
                                          String search, String dueDateFrom, String dueDateTo,
                                          String assignedDateFrom, String assignedDateTo) {
        var currentUser = SecurityUtils.getCurrentUser();
        List<Task> rawTasks = fetchAllAccessibleTasks(
                currentUser.getRole(), currentUser.getUserId(), currentUser.getBranchId());

        LocalDate dueDateFromParsed = parseDate(dueDateFrom);
        LocalDate dueDateToParsed = parseDate(dueDateTo);
        LocalDate assignedDateFromParsed = parseDate(assignedDateFrom);
        LocalDate assignedDateToParsed = parseDate(assignedDateTo);

        // Stats are filtered only by date ranges — not by status/search/priority/etc.
        List<Task> allTasks = filterTasks(rawTasks, null, null, null, null, null,
                null, null, null, dueDateFromParsed, dueDateToParsed,
                assignedDateFromParsed, assignedDateToParsed);

        LocalDate today = LocalDate.now();
        LocalDateTime thisWeekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDateTime lastWeekEnd = thisWeekStart.minusSeconds(1);

        long openTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgress = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long overdueCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        long completedThisWeek = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED
                        && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().isBefore(thisWeekStart))
                .count();
        long completedLastWeek = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED
                        && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().isBefore(lastWeekStart)
                        && !t.getUpdatedAt().isAfter(lastWeekEnd))
                .count();

        long thisWeekOpen = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO
                        && t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(thisWeekStart))
                .count();
        long lastWeekOpen = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO
                        && t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(lastWeekStart)
                        && !t.getCreatedAt().isAfter(lastWeekEnd))
                .count();

        long thisWeekInProgress = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS
                        && t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(thisWeekStart))
                .count();
        long lastWeekInProgress = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS
                        && t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(lastWeekStart)
                        && !t.getCreatedAt().isAfter(lastWeekEnd))
                .count();

        long thisWeekOverdue = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.OVERDUE
                        && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().isBefore(thisWeekStart))
                .count();
        long lastWeekOverdue = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.OVERDUE
                        && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().isBefore(lastWeekStart)
                        && !t.getUpdatedAt().isAfter(lastWeekEnd))
                .count();

        return new TaskStatsResponse(
                openTasks, computePercentChange(thisWeekOpen, lastWeekOpen),
                inProgress, computeAbsoluteChange(thisWeekInProgress - lastWeekInProgress),
                overdueCount, computeAbsoluteChange(thisWeekOverdue - lastWeekOverdue),
                completedThisWeek, computePercentChange(completedThisWeek, completedLastWeek));
    }

    private List<Task> fetchAllAccessibleTasks(String role, Long userId, Long branchId) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return taskRepository.findAllTasksForAdminList();
            case "MANAGER":
            case "BRANCH_PARTNER":
            case "ADMINISTRATIVE_ASSISTANT":
                return taskRepository.findTasksByBranchForManagerList(branchId);
            case "SENIOR_COUNSELLOR":
                List<Long> juniorIds = getJuniorCounsellorIds(userId, branchId);
                return taskRepository.findTasksForSeniorCounsellorList(branchId, userId, juniorIds);
            case "JUNIOR_COUNSELLOR":
                return taskRepository.findTasksForJuniorCounsellorList(branchId, userId);
            default:
                return List.of();
        }
    }

    private String computePercentChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        long pct = Math.round(((double) (current - previous) / previous) * 100);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    private String computeAbsoluteChange(long delta) {
        return (delta >= 0 ? "+" : "") + delta;
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        log.info("Getting task details for ID: {}", taskId);
        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        accessScopeService.requireTaskVisible(task.getBranchId(), task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);
        return convertToTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getTaskComments(Long taskId) {
        log.info("Getting comments for task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        accessScopeService.requireTaskVisible(task.getBranchId(), task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);

        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return buildCommentTree(comments);
    }

    /**
     * Groups a flat list of comments (as stored - parent_comment_id already in schema)
     * into root comments with their replies nested underneath, newest-first at every level.
     */
    private List<TaskCommentResponse> buildCommentTree(List<TaskComment> comments) {
        java.util.Map<Long, List<TaskComment>> repliesByParentId = comments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return comments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(root -> convertToTaskCommentResponseWithReplies(root, repliesByParentId))
                .collect(Collectors.toList());
    }

    private TaskCommentResponse convertToTaskCommentResponseWithReplies(TaskComment comment,
            java.util.Map<Long, List<TaskComment>> repliesByParentId) {
        TaskCommentResponse response = convertToTaskCommentResponse(comment);
        List<TaskComment> replies = repliesByParentId.getOrDefault(comment.getId(), List.of());
        response.setReplies(replies.stream()
                .map(reply -> convertToTaskCommentResponseWithReplies(reply, repliesByParentId))
                .collect(Collectors.toList()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getTaskAttachments(Long taskId) {
        log.info("Getting attachments for task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        accessScopeService.requireTaskVisible(task.getBranchId(), task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);

        return taskAttachmentRepository.findByTaskIdWithUploader(taskId).stream()
                .map(this::convertToTaskAttachmentResponse)
                .collect(Collectors.toList());
    }

    public TaskAttachmentResponse addAttachment(Long taskId, AddAttachmentRequest request, Long uploadedByUserId) {
        log.info("Adding attachment to task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User uploadedBy = userRepository.findById(uploadedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TaskAttachment attachment = new TaskAttachment(task, request.getFileName(), request.getFileUrl(),
                request.getFileSize(), uploadedBy);

        if (request.getCommentId() != null) {
            TaskComment comment = taskCommentRepository.findById(request.getCommentId())
                    .orElseThrow(() -> new RuntimeException("Comment not found"));
            if (!comment.getTask().getId().equals(taskId)) {
                throw new BusinessException("Comment does not belong to this task");
            }
            attachment.setComment(comment);
        }

        attachment.setCreatedBy(uploadedBy.getId());
        attachment.setUpdatedBy(uploadedBy.getId());

        TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);

        // Create activity log - so this shows up in the Activity tab automatically,
        // same as comments/status changes (see TaskAction.ATTACHMENT_ADDED).
        TaskActivity activity = new TaskActivity(
                task,
                TaskAction.ATTACHMENT_ADDED,
                null,
                request.getFileName(),
                uploadedBy
        );
        activity.setCreatedBy(uploadedBy.getId());
        taskActivityRepository.save(activity);

        log.info("Attachment added successfully to task {}", taskId);
        return convertToTaskAttachmentResponse(savedAttachment);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getTaskActivities(Long taskId) {
        log.info("Getting activities for task {}", taskId);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        accessScopeService.requireTaskVisible(task.getBranchId(), task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);

        List<TaskActivity> activities = taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return activities.stream()
                .map(this::convertToActivityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CombinedTaskDetailsResponse getCombinedTaskDetails(Long taskId) {
        log.info("Getting combined task details for ID: {}", taskId);
        
        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        accessScopeService.requireTaskVisible(task.getBranchId(), task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);

        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        List<TaskActivity> activities = taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId);

        CombinedTaskDetailsResponse response = new CombinedTaskDetailsResponse();
        response.setTask(convertToTaskResponse(task));
        response.setComments(comments.stream()
                .map(this::convertToTaskCommentResponse)
                .collect(Collectors.toList()));
        response.setActivities(activities.stream()
                .filter(a -> a.getAction() != TaskAction.COMMENT_ADDED)
                .map(this::convertToActivityResponse)
                .collect(Collectors.toList()));
        
        return response;
    }

    private TaskResponse convertToTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setAssigneeName(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null);
        response.setAssignerName(task.getAssignedBy() != null ? task.getAssignedBy().getFullName() : null);
        response.setDueDate(task.getDueDate());
        response.setDueTime(task.getDueTime());
        response.setCompletedAt(task.getCompletedAt());
        response.setExecutionDate(task.getExecutionDate());
        response.setSourceType(task.getSourceType());
        response.setBranchName(task.getBranch() != null ? task.getBranch().getName() : null);
        response.setReferenceType(task.getReferenceType());
        response.setReferenceId(task.getReferenceId());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setCreatedByName(task.getCreatedByUser() != null ? task.getCreatedByUser().getFullName() : null);
        response.setUpdatedBy(task.getUpdatedBy());
        response.setAssignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);
        response.setAssignedById(task.getAssignedBy() != null ? task.getAssignedBy().getId() : null);
        response.setDisplayId(task.getDisplayId());
        response.setIsDeleted(task.getIsDeleted());
        response.setBranchId(task.getBranchId());
        response.setTaskBundleId(task.getTaskBundleId());
        response.setTaskBundleName(task.getTaskBundle() != null ? task.getTaskBundle().getName() : null);
        response.setReflectStage(task.getReflectStage());
        response.setReflectState(task.getReflectState());
        response.setReflectComment(task.getReflectComment());
        response.setReflectFlaggedByName(task.getReflectFlaggedBy() != null ? task.getReflectFlaggedBy().getFullName() : null);
        response.setReflectFlaggedAt(task.getReflectFlaggedAt());
        response.setReflectResubmittedAt(task.getReflectResubmittedAt());
        response.setCurrentStep(task.getCurrentStep());
        response.setNextStep(task.getNextStep());
        return response;
    }

    private TaskCommentResponse convertToTaskCommentResponse(TaskComment comment) {
        TaskCommentResponse response = new TaskCommentResponse();
        response.setId(comment.getId());
        response.setComment(comment.getComment());
        response.setCommentedById(comment.getCommentedBy() != null ? comment.getCommentedBy().getId() : null);
        response.setCommentedByName(comment.getCommentedBy() != null ? comment.getCommentedBy().getFullName() : null);
        response.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        response.setCreatedAt(comment.getCreatedAt());
        response.setEdited(comment.isEdited());
        return response;
    }

    private TaskAttachmentResponse convertToTaskAttachmentResponse(TaskAttachment attachment) {
        TaskAttachmentResponse response = new TaskAttachmentResponse();
        response.setId(attachment.getId());
        response.setTaskId(attachment.getTask() != null ? attachment.getTask().getId() : null);
        response.setFileName(attachment.getFileName());
        response.setFileUrl(attachment.getFileUrl());
        response.setFileSize(attachment.getFileSize());
        response.setFileSizeFormatted(formatFileSize(attachment.getFileSize()));
        response.setUploadedById(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getId() : null);
        response.setUploadedByName(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getFullName() : null);
        response.setUploadedAt(attachment.getUploadedAt());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private ActivityResponse convertToActivityResponse(TaskActivity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setAction(activity.getAction());
        response.setOldValue(activity.getOldValue());
        response.setNewValue(activity.getNewValue());
        response.setComment(activity.getComment());
        response.setDoneByName(activity.getDoneBy() != null ? activity.getDoneBy().getFullName() : null);
        response.setCreatedAt(activity.getCreatedAt());
        response.setMessage(formatActivityMessage(activity));
        return response;
    }

    private String formatActivityMessage(TaskActivity activity) {
        String actorName = activity.getDoneBy() != null ? activity.getDoneBy().getFullName() : "Unknown";
        
        switch (activity.getAction()) {
            case CREATED:
                return actorName + " created this task";
            case STATUS_CHANGED:
                // oldValue/newValue both null marks a status-adjacent event that isn't itself
                // a status transition (e.g. a reviewer resolving a re-review) - render just
                // the comment rather than the misleading "changed status from null to null".
                if (activity.getOldValue() == null && activity.getNewValue() == null) {
                    return activity.getComment() != null && !activity.getComment().isBlank()
                            ? actorName + " " + activity.getComment()
                            : actorName + " updated this task";
                }
                String base = actorName + " changed status from " + activity.getOldValue() + " to " + activity.getNewValue();
                return activity.getComment() != null && !activity.getComment().isBlank()
                        ? base + " — " + activity.getComment()
                        : base;
            case PRIORITY_CHANGED:
                return actorName + " changed priority from " + activity.getOldValue() + " to " + activity.getNewValue();
            case ASSIGNED:
                return actorName + " assigned task from " + (activity.getOldValue() != null ? activity.getOldValue() : "Unassigned") + " to " + activity.getNewValue();
            case COMMENT_ADDED:
                return actorName + " added a comment";
            case DUE_DATE_UPDATED:
                return actorName + " changed due date from " + (activity.getOldValue() != null ? activity.getOldValue() : "Not set") + " to " + activity.getNewValue();
            case ATTACHMENT_ADDED:
                return actorName + " attached " + activity.getNewValue();
            case MARKED_OVERDUE:
                return actorName + " changed status from " + activity.getOldValue() + " to " + activity.getNewValue()
                        + " — automatically changed due to passed due date";
            default:
                return actorName + " performed " + activity.getAction();
        }
    }

    /**
     * Get junior counsellor IDs assigned to a senior counsellor in the same branch
     */
    private List<Long> getJuniorCounsellorIds(Long seniorCounsellorId, Long branchId) {
        log.info("Getting junior counsellor IDs for senior: {} in branch: {}", seniorCounsellorId, branchId);
        
        // Fetch users WHERE reporting_manager_id = seniorId AND role = 'JUNIOR_COUNSELLOR' AND branch_id = given branchId
        List<Long> juniorIds = userRepository.findJuniorCounsellorIdsBySeniorIdAndBranchId(seniorCounsellorId, branchId);
        
        log.info("Found {} junior counsellors for senior: {} in branch: {}", juniorIds.size(), seniorCounsellorId, branchId);
        return juniorIds;
    }

    /**
     * Validates task update permissions with strict branch security and defensive validation
     * 
     * Update Rules:
     * - ADMIN: can update any task
     * - MANAGER: can update tasks WHERE branchId = user.branchId
     * - SENIOR_COUNSELLOR: can update tasks WHERE branchId = user.branchId AND (assignedTo = user.id OR assignedTo IN juniorIds)
     * - JUNIOR_COUNSELLOR: can update tasks WHERE branchId = user.branchId AND assignedTo = user.id
     */
    private void validateTaskUpdate(User updatedBy, Task task) {
        String userRole = updatedBy.getRole() != null ? updatedBy.getRole().getName() : null;
        Long userId = updatedBy.getId();
        Long userBranchId = updatedBy.getBranch() != null ? updatedBy.getBranch().getId() : null;
        Long taskBranchId = task.getBranch() != null ? task.getBranch().getId() : null;
        Long assignedToId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
        
        log.info("Validating task update: {} ({}) updating task {} assigned to {} in branch {}", 
            updatedBy.getFullName(), userRole, task.getId(), assignedToId, taskBranchId);
        
        // Defensive validation: Check for null values
        if (userRole == null) {
            throw new UnauthorizedAccessException("User role not properly configured");
        }
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Task cannot be null or must have a valid ID");
        }
        
        // Branch security enforcement: Cannot update tasks outside user's branch
        if (!"ADMIN".equals(userRole.toUpperCase())) {
            if (userBranchId == null) {
                throw new BranchSecurityException("User must be assigned to a branch to update tasks");
            }
            if (taskBranchId == null) {
                throw new BranchSecurityException("Task must be assigned to a branch");
            }
            if (!userBranchId.equals(taskBranchId)) {
                throw new BranchSecurityException("Cannot update tasks in different branches. User branch: " + 
                    userBranchId + ", Task branch: " + taskBranchId);
            }
        }
        
        switch (userRole.toUpperCase()) {
            case "ADMIN":
                // Admin can update any task (branch restriction doesn't apply)
                log.info("ADMIN user ID {} updating task {}", updatedBy.getId(), task.getId());
                break;
                
            case "MANAGER":
            case "ADMINISTRATIVE_ASSISTANT":
                // Manager can update tasks WHERE branchId = user.branchId (already validated above)
                log.info("MANAGER user ID {} updating task {} in branch {}", updatedBy.getId(), task.getId(), userBranchId);
                break;
                
            case "SENIOR_COUNSELLOR":
                // Senior Counsellor can update tasks WHERE branchId = user.branchId AND (assignedTo = user.id OR assignedTo IN juniorIds)
                if (!userId.equals(assignedToId)) {
                    List<Long> juniorIds = getJuniorCounsellorIds(userId, userBranchId);
                    if (!juniorIds.contains(assignedToId)) {
                        throw new UnauthorizedAccessException("SENIOR_COUNSELLOR can only update their own tasks or tasks assigned to their junior counsellors");
                    }
                }
                log.info("SENIOR_COUNSELLOR user ID {} updating task {} in branch {}", updatedBy.getId(), task.getId(), userBranchId);
                break;
                
            case "JUNIOR_COUNSELLOR":
            case "VIDEO_EDITOR":
            case "GRAPHIC_DESIGNER":
            case "WEB_DEV":
            case "WEB_DEVELOPER":
                // Individual-contributor employee roles (Employee Tree's non-counsellor
                // branches, e.g. "Video Editors") can update tasks WHERE branchId =
                // user.branchId AND assignedTo = user.id - same self-only rule as Junior
                // Counsellor. Without this, Part D1 (employee changes their own task's
                // status) throws "cannot update tasks" for every non-counsellor employee.
                if (!userId.equals(assignedToId)) {
                    throw new UnauthorizedAccessException(userRole + " can only update their own tasks");
                }
                log.info("{} user ID {} updating their own task {}", userRole, updatedBy.getId(), task.getId());
                break;

            default:
                // Other roles cannot update tasks
                throw new UnauthorizedAccessException(userRole + " cannot update tasks");
        }
    }

    /**
     * Validates status transition control with enforced workflow rules.
     *
     * Valid Status Flow: every task (manual or template-generated, see Task's
     * constructors / TemplateInstantiationService) is seeded at TODO and follows
     * TODO → IN_PROGRESS → DONE, with OVERDUE reachable from TODO/IN_PROGRESS by
     * the overdue scheduler and still resumable by the employee, and REFLECT reachable
     * only via the Day Approval Workflow's SEND_BACK action (DayApprovalService), never
     * directly through this employee-facing endpoint - REFLECT is resumed back to
     * IN_PROGRESS once the employee has addressed the reviewer's comment.
     *
     * (PENDING used to be a separate legacy starting state duplicating TODO's "not
     * started" meaning with a stricter transition rule of its own; it was retired and
     * every task now goes through the single TODO-first flow above.)
     *
     * Rules:
     * - Terminal states (DONE/COMPLETED/VERIFIED) cannot be modified by non-admins.
     * - REFLECT cannot be set directly by an employee - only by the approval workflow.
     * - VERIFIED cannot be set directly through this method by ANYONE, including ADMIN -
     *   only DayApprovalService#approveDayLevel's cascade stamps it, and only once the day
     *   itself has reached ADMIN_VERIFIED. No override here, unlike every other rule below -
     *   otherwise ADMIN's blanket bypass would let this endpoint verify a task whose day was
     *   never reviewed at all.
     * - ADMIN can override any other transition.
     */
    private void validateStatusTransition(TaskStatus oldStatus, TaskStatus newStatus, String userRole) {
        log.info("Validating status transition: {} → {} by user role: {}", oldStatus, newStatus, userRole);

        // Allow same status (no change) - checked first, before the VERIFIED guard below, so
        // an idempotent PATCH re-affirming an already-VERIFIED task's current status stays a
        // harmless no-op rather than tripping it.
        if (oldStatus == newStatus) {
            return;
        }

        // VERIFIED has exactly one door - DayApprovalService#approveDayLevel's cascade, run
        // only once the day itself reaches ADMIN_VERIFIED - and no exceptions, not even
        // ADMIN's usual override below. Checked before that override so it can never be
        // skipped: without this, ADMIN could stamp VERIFIED on any DONE task through this
        // employee-facing endpoint regardless of whether the day was ever reviewed.
        if (newStatus == TaskStatus.VERIFIED) {
            throw new InvalidStatusTransitionException(
                    "VERIFIED can only be stamped by the day-approval cascade when Admin approves the day, not set directly");
        }

        // Admin can override any other transition
        if ("ADMIN".equals(userRole.toUpperCase())) {
            log.info("ADMIN overriding status transition: {} → {}", oldStatus, newStatus);
            return;
        }

        // REFLECT is only ever entered via the Day Approval Workflow's SEND_BACK action
        // (see DayApprovalService), which bypasses this method entirely and writes the
        // status directly - never as a target an employee can PATCH to themselves.
        if (newStatus == TaskStatus.REFLECT) {
            throw new InvalidStatusTransitionException(
                    "REFLECT can only be set by a reviewer sending a day back for rework, not directly by an employee");
        }

        switch (oldStatus) {
            case TODO:
                // Employee can pick the task up, or mark it straight to done without
                // passing through IN_PROGRESS.
                if (newStatus != TaskStatus.IN_PROGRESS && newStatus != TaskStatus.DONE && newStatus != TaskStatus.COMPLETED) {
                    throw new InvalidStatusTransitionException("TODO tasks can only transition to IN_PROGRESS or DONE");
                }
                break;

            case IN_PROGRESS:
                if (newStatus != TaskStatus.COMPLETED && newStatus != TaskStatus.DONE) {
                    throw new InvalidStatusTransitionException("IN_PROGRESS tasks can only transition to DONE");
                }
                break;

            case OVERDUE:
                // Still resumable: the employee can pick an overdue task back up and carry
                // on, or mark it straight to done.
                if (newStatus != TaskStatus.IN_PROGRESS && newStatus != TaskStatus.DONE && newStatus != TaskStatus.COMPLETED) {
                    throw new InvalidStatusTransitionException("OVERDUE tasks can only transition to IN_PROGRESS or DONE");
                }
                break;

            case REFLECT:
                // Employee addresses the reviewer's comment and resumes work, or marks it
                // straight to done without passing back through IN_PROGRESS first.
                if (newStatus != TaskStatus.IN_PROGRESS && newStatus != TaskStatus.DONE && newStatus != TaskStatus.COMPLETED) {
                    throw new InvalidStatusTransitionException("REFLECT tasks can only transition to IN_PROGRESS or DONE");
                }
                break;

            case COMPLETED:
            case DONE:
            case VERIFIED:
                // Terminal states should not transition back (except ADMIN, handled above).
                // VERIFIED is never a valid newStatus from any other case above either, so an
                // employee can never PATCH their way into it directly - only
                // DayApprovalService#approveDayLevel sets it, once the day reaches
                // ADMIN_VERIFIED and the task isn't mid re-review.
                throw new InvalidStatusTransitionException(oldStatus + " tasks cannot be modified (only ADMIN can override)");

            default:
                throw new InvalidStatusTransitionException("Unknown status: " + oldStatus);
        }

        log.info("Status transition validated: {} → {}", oldStatus, newStatus);
    }

    /**
     * Validates task creation permissions based on user roles
     * 
     * Creation Rules:
     * - ADMIN: can create task for anyone
     * - MANAGER: can create task ONLY for users in their branch
     * - SENIOR_COUNSELLOR: can create task ONLY for assignedTo IN (juniorIds under this senior) AND branchId = user.branchId
     * - JUNIOR_COUNSELLOR: not allowed
     */
    private void validateTaskCreation(User createdBy, User assignedTo) {
        String creatorRole = createdBy.getRole() != null ? createdBy.getRole().getName() : null;
        String assigneeRole = assignedTo.getRole() != null ? assignedTo.getRole().getName() : null;
        
        log.info("Validating task creation: creatorId={} ({}) -> assigneeId={} ({})",
            createdBy.getId(), creatorRole, assignedTo.getId(), assigneeRole);
        
        if (creatorRole == null || assigneeRole == null) {
            throw new BusinessException("User roles not properly configured");
        }
        
        switch (creatorRole.toUpperCase()) {
            case "ADMIN":
                // Admin can create task for anyone
                log.info("ADMIN user ID {} creating task for assignee ID {} ({})", createdBy.getId(), assignedTo.getId(), assigneeRole);
                break;
                
            case "MANAGER":
            case "ADMINISTRATIVE_ASSISTANT":
                // Manager can create task ONLY for users in their branch
                if (createdBy.getBranch() == null || assignedTo.getBranch() == null) {
                    throw new BusinessException("MANAGER must have a branch assignment to create tasks");
                }
                if (!createdBy.getBranch().getId().equals(assignedTo.getBranch().getId())) {
                    throw new BusinessException("MANAGER can only create tasks for users in the same branch");
                }
                if ("ADMIN".equals(assigneeRole)) {
                    throw new BusinessException("MANAGER cannot create tasks for ADMIN users");
                }
                log.info("MANAGER user ID {} creating task for assignee ID {} ({}) in branch {}",
                    createdBy.getId(), assignedTo.getId(), assigneeRole, createdBy.getBranch().getId());
                break;
                
            case "SENIOR_COUNSELLOR":
                // Senior Counsellor can create task ONLY for assignedTo IN (juniorIds under this senior) AND branchId = user.branchId
                if (!"JUNIOR_COUNSELLOR".equals(assigneeRole)) {
                    throw new BusinessException("SENIOR_COUNSELLOR can only create tasks for JUNIOR_COUNSELLOR");
                }
                if (createdBy.getBranch() == null || assignedTo.getBranch() == null) {
                    throw new BusinessException("SENIOR_COUNSELLOR must have a branch assignment to create tasks");
                }
                if (!createdBy.getBranch().getId().equals(assignedTo.getBranch().getId())) {
                    throw new BusinessException("SENIOR_COUNSELLOR can only create tasks for JUNIOR_COUNSELLOR in the same branch");
                }
                // TODO: Add validation to ensure assignedTo is actually assigned under this senior
                log.info("SENIOR_COUNSELLOR user ID {} creating task for assignee ID {} ({}) in branch {}",
                    createdBy.getId(), assignedTo.getId(), assigneeRole, createdBy.getBranch().getId());
                break;
                
            case "JUNIOR_COUNSELLOR":
            case "VIDEO_EDITOR":
            case "STUDENT":
            case "EMPLOYEE":
            case "REFERRAL":
            case "COMPANY":
                // These roles cannot create tasks
                throw new BusinessException(creatorRole + " cannot create tasks");
                
            default:
                throw new BusinessException("Unknown role: " + creatorRole);
        }
    }

    /**
     * Validates task assignment permissions with strict role-based rules and branch security
     * 
     * Assignment Rules:
     * - ADMIN: Can assign to anyone
     * - MANAGER: Can assign ONLY to SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR, Video Editor, Web Developer
     * - SENIOR_COUNSELLOR: Can assign ONLY to their own JUNIOR_COUNSELLORS, cannot assign to themselves
     * - JUNIOR_COUNSELLOR: Cannot assign tasks
     */
    private void validateTaskAssignment(User assignedBy, User assignedTo) {
        String assignerRole = assignedBy.getRole() != null ? assignedBy.getRole().getName() : null;
        String assigneeRole = assignedTo.getRole() != null ? assignedTo.getRole().getName() : null;
        
        log.info("Validating task assignment: assignerID={} ({}) -> assigneeId={} ({})",
            assignedBy.getId(), assignerRole, assignedTo.getId(), assigneeRole);
        
        if (assignerRole == null || assigneeRole == null) {
            throw new UnauthorizedAccessException("User roles not properly configured");
        }
        
        // Branch security enforcement: assignedUser.branchId must equal currentUser.branchId
        Long assignerBranchId = assignedBy.getBranchId();
        Long assigneeBranchId = assignedTo.getBranchId();
        
        if (assignerBranchId == null || !assignerBranchId.equals(assigneeBranchId)) {
            throw new BranchSecurityException("Cannot assign tasks to users in different branches. Assigner branch: " + 
                assignerBranchId + ", Assignee branch: " + assigneeBranchId);
        }
        
        switch (assignerRole.toUpperCase()) {
            case "ADMIN":
                // Admin can assign to anyone
                log.info("ADMIN user ID {} assigning task to assignee ID {} ({})", assignedBy.getId(), assignedTo.getId(), assigneeRole);
                break;
                
            case "MANAGER":
            case "BRANCH_PARTNER":
            case "ADMINISTRATIVE_ASSISTANT":
                // Manager/Branch Partner can assign ONLY to: SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR, Video Editor, Web Developer
                // Cannot assign to ADMIN, MANAGER, or other BRANCH_PARTNER
                String roleDisplay = "MANAGER".equals(assignerRole) ? "MANAGER" : "BRANCH_PARTNER";
                if ("ADMIN".equals(assigneeRole) || "MANAGER".equals(assigneeRole) || "BRANCH_PARTNER".equals(assigneeRole)) {
                    throw new InvalidAssignmentException(roleDisplay + " cannot assign tasks to ADMIN, MANAGER, or other BRANCH_PARTNER users");
                }
                if (!"SENIOR_COUNSELLOR".equals(assigneeRole) && 
                    !"JUNIOR_COUNSELLOR".equals(assigneeRole) && 
                    !"VIDEO_EDITOR".equals(assigneeRole) && 
                    !"WEB_DEVELOPER".equals(assigneeRole)) {
                    throw new InvalidAssignmentException(roleDisplay + " can only assign tasks to SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR, Video Editor, or Web Developer");
                }
                log.info("{} user ID {} assigning task to assignee ID {} ({})", assignerRole, assignedBy.getId(), assignedTo.getId(), assigneeRole);
                break;
                
            case "SENIOR_COUNSELLOR":
                // Self-task restriction: Cannot assign to themselves
                if (assignedBy.getId().equals(assignedTo.getId())) {
                    throw new InvalidAssignmentException("SENIOR_COUNSELLOR cannot assign tasks to themselves");
                }
                
                // Can assign ONLY to their own JUNIOR_COUNSELLORS
                if (!"JUNIOR_COUNSELLOR".equals(assigneeRole)) {
                    throw new InvalidAssignmentException("SENIOR_COUNSELLOR can only assign tasks to JUNIOR_COUNSELLOR");
                }
                
                // Verify that the assigned junior counsellor reports to this senior
                List<Long> juniorIds = getJuniorCounsellorIds(assignedBy.getId(), assignerBranchId);
                if (!juniorIds.contains(assignedTo.getId())) {
                    throw new InvalidAssignmentException("SENIOR_COUNSELLOR can only assign tasks to their own junior counsellors");
                }
                
                log.info("SENIOR_COUNSELLOR user ID {} assigning task to junior counsellor ID {}",
                    assignedBy.getId(), assignedTo.getId());
                break;
                
            case "JUNIOR_COUNSELLOR":
            case "VIDEO_EDITOR":
            case "WEB_DEVELOPER":
            case "STUDENT":
            case "EMPLOYEE":
            case "REFERRAL":
            case "COMPANY":
                // These roles cannot assign tasks
                throw new UnauthorizedAccessException(assignerRole + " cannot assign tasks to other users");
                
            default:
                throw new UnauthorizedAccessException("Unknown role: " + assignerRole);
        }
    }
}
