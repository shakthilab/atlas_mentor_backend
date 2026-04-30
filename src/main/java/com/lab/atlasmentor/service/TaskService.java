package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("Assignee user not found"));

        User assignedBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Creator user not found"));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                assignedTo,
                assignedBy,
                request.getPriority() != null ? request.getPriority() : Priority.MEDIUM,
                request.getDueDate(),
                branch
        );

        task.setReferenceType(request.getReferenceType());
        task.setReferenceId(request.getReferenceId());
        task.setCreatedBy(assignedBy);
        task.setUpdatedBy(assignedBy);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.CREATED,
                null,
                null,
                assignedBy
        );
        activity.setCreatedBy(assignedBy);
        taskActivityRepository.save(activity);

        log.info("Task created successfully with ID: {}", savedTask.getId());
        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTaskStatus(Long taskId, TaskStatus newStatus, Long updatedByUserId) {
        log.info("Updating task {} status to {}", taskId, newStatus);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        TaskStatus oldStatus = task.getStatus();
        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus(newStatus);
        task.setUpdatedBy(updatedBy);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.STATUS_CHANGED,
                oldStatus.toString(),
                newStatus.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy);
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

        String oldAssigneeName = task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null;
        String newAssigneeName = newAssignee.getFullName();

        task.setAssignedTo(newAssignee);
        task.setUpdatedBy(assignedBy);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.ASSIGNED,
                oldAssigneeName,
                newAssigneeName,
                assignedBy
        );
        activity.setCreatedBy(assignedBy);
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
        comment.setCreatedBy(commentedBy);
        comment.setUpdatedBy(commentedBy);

        TaskComment savedComment = taskCommentRepository.save(comment);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                task,
                TaskAction.COMMENT_ADDED,
                null,
                null,
                commentedBy
        );
        activity.setCreatedBy(commentedBy);
        taskActivityRepository.save(activity);

        log.info("Comment added successfully to task {}", taskId);
        return convertToTaskCommentResponse(savedComment);
    }

    public TaskResponse updateTaskPriority(Long taskId, Priority newPriority, Long updatedByUserId) {
        log.info("Updating task {} priority to {}", taskId, newPriority);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Priority oldPriority = task.getPriority();
        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setPriority(newPriority);
        task.setUpdatedBy(updatedBy);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.PRIORITY_CHANGED,
                oldPriority.toString(),
                newPriority.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy);
        taskActivityRepository.save(activity);

        log.info("Task {} priority updated successfully", taskId);
        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTaskDueDate(Long taskId, LocalDate newDueDate, Long updatedByUserId) {
        log.info("Updating task {} due date to {}", taskId, newDueDate);

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        LocalDate oldDueDate = task.getDueDate();
        User updatedBy = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setDueDate(newDueDate);
        task.setUpdatedBy(updatedBy);

        Task savedTask = taskRepository.save(task);

        // Create activity log
        TaskActivity activity = new TaskActivity(
                savedTask,
                TaskAction.DUE_DATE_UPDATED,
                oldDueDate != null ? oldDueDate.toString() : null,
                newDueDate.toString(),
                updatedBy
        );
        activity.setCreatedBy(updatedBy);
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

        task.setIsDeleted(true);
        task.setUpdatedBy(deletedBy);

        taskRepository.save(task);

        log.info("Task {} soft deleted successfully", taskId);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        log.info("Getting all tasks");
        return taskRepository.findAllActiveTasks().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksWithFilters(TaskStatus status, Long assigneeId, Long branchId, 
                                                  Priority priority, Long createdBy, String keyword, Boolean overdue) {
        log.info("Getting tasks with filters: status={}, assigneeId={}, branchId={}, priority={}, createdBy={}, keyword={}, overdue={}", 
                status, assigneeId, branchId, priority, createdBy, keyword, overdue);
        
        return taskRepository.findTasksWithFilters(status, assigneeId, branchId, priority, createdBy, keyword, overdue)
                .stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
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
}
