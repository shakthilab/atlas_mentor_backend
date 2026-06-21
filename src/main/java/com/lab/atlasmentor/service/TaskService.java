package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
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
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

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
        task.setCreatedBy(createdBy.getId());
        task.setUpdatedBy(createdBy.getId());

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

        task.setStatus(newStatus);
        task.setUpdatedBy(updatedBy.getId());

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
        } else if ("MANAGER".equals(currentUser.getRole()) || "BRANCH_PARTNER".equals(currentUser.getRole())) {
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
        } else if ("MANAGER".equals(currentUser.getRole()) || "BRANCH_PARTNER".equals(currentUser.getRole())) {
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

        long openTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
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
                .filter(t -> t.getStatus() == TaskStatus.PENDING
                        && t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(thisWeekStart))
                .count();
        long lastWeekOpen = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING
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
        return convertToTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getTaskComments(Long taskId) {
        log.info("Getting comments for task {}", taskId);
        
        // Verify task exists
        taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return comments.stream()
                .map(this::convertToTaskCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getTaskActivities(Long taskId) {
        log.info("Getting activities for task {}", taskId);
        
        // Verify Task exists
        taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
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
        response.setBranchName(task.getBranch() != null ? task.getBranch().getName() : null);
        response.setReferenceType(task.getReferenceType());
        response.setReferenceId(task.getReferenceId());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    private TaskCommentResponse convertToTaskCommentResponse(TaskComment comment) {
        TaskCommentResponse response = new TaskCommentResponse();
        response.setId(comment.getId());
        response.setComment(comment.getComment());
        response.setCommentedByName(comment.getCommentedBy() != null ? comment.getCommentedBy().getFullName() : null);
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    private ActivityResponse convertToActivityResponse(TaskActivity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setAction(activity.getAction());
        response.setOldValue(activity.getOldValue());
        response.setNewValue(activity.getNewValue());
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
                return actorName + " changed status from " + activity.getOldValue() + " to " + activity.getNewValue();
            case PRIORITY_CHANGED:
                return actorName + " changed priority from " + activity.getOldValue() + " to " + activity.getNewValue();
            case ASSIGNED:
                return actorName + " assigned task from " + (activity.getOldValue() != null ? activity.getOldValue() : "Unassigned") + " to " + activity.getNewValue();
            case COMMENT_ADDED:
                return actorName + " added a comment";
            case DUE_DATE_UPDATED:
                return actorName + " changed due date from " + (activity.getOldValue() != null ? activity.getOldValue() : "Not set") + " to " + activity.getNewValue();
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
                // Junior Counsellor can update tasks WHERE branchId = user.branchId AND assignedTo = user.id
                if (!userId.equals(assignedToId)) {
                    throw new UnauthorizedAccessException("JUNIOR_COUNSELLOR can only update their own tasks");
                }
                log.info("JUNIOR_COUNSELLOR user ID {} updating their own task {}", updatedBy.getId(), task.getId());
                break;
                
            default:
                // Other roles cannot update tasks
                throw new UnauthorizedAccessException(userRole + " cannot update tasks");
        }
    }

    /**
     * Validates status transition control with enforced workflow rules
     * 
     * Valid Status Flow:
     * PENDING → IN_PROGRESS → COMPLETED
     *
     * Rules:
     * - Cannot move COMPLETED → IN_PROGRESS (except ADMIN)
     * - Cannot skip flow randomly
     * - ADMIN can override
     */
    private void validateStatusTransition(TaskStatus oldStatus, TaskStatus newStatus, String userRole) {
        log.info("Validating status transition: {} → {} by user role: {}", oldStatus, newStatus, userRole);

        // Admin can override any transition
        if ("ADMIN".equals(userRole.toUpperCase())) {
            log.info("ADMIN overriding status transition: {} → {}", oldStatus, newStatus);
            return;
        }

        // Allow same status (no change)
        if (oldStatus == newStatus) {
            return;
        }

        // Define valid transitions
        switch (oldStatus) {
            case PENDING:
                if (newStatus != TaskStatus.IN_PROGRESS) {
                    throw new InvalidStatusTransitionException("PENDING tasks can only transition to IN_PROGRESS");
                }
                break;

            case IN_PROGRESS:
                if (newStatus != TaskStatus.COMPLETED) {
                    throw new InvalidStatusTransitionException("IN_PROGRESS tasks can only transition to COMPLETED");
                }
                break;

            case COMPLETED:
                // COMPLETED should not transition back (except ADMIN which is handled above)
                throw new InvalidStatusTransitionException("COMPLETED tasks cannot be modified (only ADMIN can override)");

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
