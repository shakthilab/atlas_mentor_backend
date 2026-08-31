package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.enums.TaskSource;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for generating tasks from task bundles.
 * Creates tasks for users based on their roles and bundle configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskGenerationService {
    
    private final TaskBundleTaskRepository taskBundleTaskRepository;
    private final TaskBundleRepository taskBundleRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskDisplayIdService taskDisplayIdService;
    
    // Admin user ID for auto-generated tasks
    private static final Long ADMIN_USER_ID = 1L;
    
    /**
     * Generate tasks for all users with the role mapped to the bundle.
     * REQUIRES_NEW so that task generation failures never mark the caller's
     * bundle-execution-tracking transaction as rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenerationResult generateTasksForBundle(TaskBundle bundle, LocalDate executionDate) {
        log.info("Generating tasks for bundle: {} on date: {}", bundle.getName(), executionDate);
        
        // Get active tasks in the bundle
        List<TaskBundleTask> bundleTasks = taskBundleTaskRepository.findActiveByTaskBundleId(bundle.getId());
        if (bundleTasks.isEmpty()) {
            log.warn("No active tasks found in bundle: {}", bundle.getId());
            return new GenerationResult(0, 0);
        }
        
        // Get all users with the bundle's role
        List<User> targetUsers = userRepository.findByRoleIdAndStatus(
                bundle.getRole().getId(), com.lab.atlasmentor.enums.UserStatus.ACTIVE);
        
        if (targetUsers.isEmpty()) {
            log.warn("No active users found with role: {}", bundle.getRole().getId());
            return new GenerationResult(0, 0);
        }
        
        int totalTasksGenerated = 0;
        
        for (User user : targetUsers) {
            try {
                int tasksForUser = generateTasksForUser(bundle, bundleTasks, user, executionDate);
                totalTasksGenerated += tasksForUser;
            } catch (Exception e) {
                log.error("Error generating tasks for user {} in bundle {}: {}", 
                        user.getId(), bundle.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Task generation completed for bundle {}: {} users, {} tasks generated", 
                bundle.getId(), targetUsers.size(), totalTasksGenerated);
        
        return new GenerationResult(targetUsers.size(), totalTasksGenerated);
    }
    
    /**
     * Generate tasks for a specific user from a bundle.
     * REQUIRES_NEW so a single user failure doesn't roll back the parent
     * generateTasksForBundle transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int generateTasksForUser(TaskBundle bundle, List<TaskBundleTask> bundleTasks, User user, LocalDate executionDate) {
        log.debug("Generating tasks for user {} from bundle {}", user.getId(), bundle.getId());
        
        int tasksGenerated = 0;
        User adminUser = userRepository.findById(ADMIN_USER_ID)
                .orElseThrow(() -> new RuntimeException("Admin user not found with ID: " + ADMIN_USER_ID));
        
        for (TaskBundleTask bundleTask : bundleTasks) {
            try {
                Task task = createTaskFromBundleTask(bundleTask, bundle, user, adminUser, executionDate);
                taskRepository.save(task);
                
                // Create task activity for auto-generation
                createTaskActivity(task, "AUTO_GENERATED", adminUser.getId());
                
                tasksGenerated++;
                
            } catch (Exception e) {
                log.error("Error creating task from bundle task {} for user {}: {}", 
                        bundleTask.getId(), user.getId(), e.getMessage(), e);
            }
        }
        
        return tasksGenerated;
    }
    
    /**
     * Create a task entity from a bundle task
     */
    private Task createTaskFromBundleTask(TaskBundleTask bundleTask, TaskBundle bundle, 
                                       User assignedTo, User assignedBy, LocalDate executionDate) {
        Task task = new Task();
        task.setTitle(bundleTask.getTitle());
        task.setDescription(bundleTask.getDescription());
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(assignedBy);
        task.setCreatedByUser(assignedBy);
        task.setPriority(bundleTask.getPriority());
        task.setStatus(TaskStatus.TODO);
        task.setTaskBundle(bundle);
        task.setSourceType(TaskSource.TASK_BUNDLE);
        task.setExecutionDate(executionDate);
        task.setIsDeleted(false);
        task.setDisplayId(taskDisplayIdService.nextDisplayId(bundle.getRole()));
        // current_step/next_step (V18) - this legacy path never attaches a day workspace,
        // so both stay null; see DayApprovalService#applyStepLabels.
        DayApprovalService.applyStepLabels(task);
        
        // Set due date: use defaultDueDays if configured, otherwise fall back to execution date
        if (bundleTask.getDefaultDueDays() != null && bundleTask.getDefaultDueDays() > 0) {
            task.setDueDate(executionDate.plusDays(bundleTask.getDefaultDueDays()));
        } else {
            task.setDueDate(executionDate);
        }
        
        // Set branch from user
        if (assignedTo.getBranch() != null) {
            task.setBranch(assignedTo.getBranch());
        }
        
        // Set audit fields
        task.setCreatedBy(assignedBy.getId());
        task.setUpdatedBy(assignedBy.getId());
        
        return task;
    }
    
    /**was already executed today, skipping
     * Create task activity record
     */
    private void createTaskActivity(Task task, String action, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        TaskActivity activity = new TaskActivity();
        activity.setTask(task);
        activity.setAction(com.lab.atlasmentor.enums.TaskAction.valueOf(action));
        activity.setCreatedBy(userId);
        activity.setUpdatedBy(userId);
        activity.setDoneBy(user);
        
        taskActivityRepository.save(activity);
    }
    
    /**
     * Manually trigger task generation for a specific bundle and date
     */
    @Transactional
    public GenerationResult manuallyGenerateTasks(Long bundleId, LocalDate executionDate) {
        TaskBundle bundle = taskBundleRepository.findById(bundleId)
                .orElseThrow(() -> new RuntimeException("Task bundle not found with ID: " + bundleId));
        
        if (!bundle.isActive()) {
            throw new BusinessException("Task bundle is not active or is deleted");
        }
        
        return generateTasksForBundle(bundle, executionDate);
    }
    
    /**
     * Generate tasks for a specific user from all active bundles for their role
     */
    @Transactional
    public GenerationResult generateTasksForUserByRole(Long userId, LocalDate executionDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (user.getRole() == null) {
            throw new BusinessException("User has no role assigned");
        }
        
        // Get all active bundles for user's role
        List<TaskBundle> activeBundles = taskBundleRepository.findActiveByRole(
                user.getRole().getId(), com.lab.atlasmentor.enums.BundleStatus.ACTIVE);
        
        int totalUsers = 1; // Only this user
        int totalTasksGenerated = 0;
        
        for (TaskBundle bundle : activeBundles) {
            try {
                // Check if bundle should execute today
                if (bundle.getSchedule() != null && 
                    bundle.getSchedule().shouldExecuteToday(executionDate)) {
                    
                    List<TaskBundleTask> bundleTasks = taskBundleTaskRepository.findActiveByTaskBundleId(bundle.getId());
                    int tasksForUser = generateTasksForUser(bundle, bundleTasks, user, executionDate);
                    totalTasksGenerated += tasksForUser;
                    
                    log.info("Generated {} tasks for user {} from bundle {}", 
                            tasksForUser, userId, bundle.getId());
                }
            } catch (Exception e) {
                log.error("Error generating tasks for user {} from bundle {}: {}", 
                        userId, bundle.getId(), e.getMessage(), e);
            }
        }
        
        return new GenerationResult(totalUsers, totalTasksGenerated);
    }
    
    /**
     * Check for duplicate tasks before generation
     */
    @Transactional(readOnly = true)
    public boolean hasDuplicateTask(Long bundleId, Long userId, LocalDate executionDate, String taskTitle) {
        List<Task> existingTasks = taskRepository.findByAssignedToIdAndTaskBundleIdAndExecutionDateAndTitleAndIsDeletedFalse(
                userId, bundleId, executionDate, taskTitle);
        
        return !existingTasks.isEmpty();
    }
    
    /**
     * Get task generation statistics for a date range
     */
    @Transactional(readOnly = true)
    public TaskGenerationStats getGenerationStats(LocalDate startDate, LocalDate endDate) {
        List<Task> generatedTasks = taskRepository.findBySourceTypeAndExecutionDateBetweenAndIsDeletedFalse(
                TaskSource.TASK_BUNDLE, startDate, endDate);
        
        TaskGenerationStats stats = new TaskGenerationStats();
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        stats.setTotalTasksGenerated(generatedTasks.size());
        
        // Count by date
        generatedTasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(task -> task.getExecutionDate()))
                .forEach((date, tasks) -> {
                    stats.addDailyCount(date, tasks.size());
                });
        
        // Count by bundle
        generatedTasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(task -> task.getTaskBundle().getId()))
                .forEach((bundleId, tasks) -> {
                    stats.addBundleCount(bundleId, tasks.size());
                });
        
        return stats;
    }
    
    /**
     * Result class for task generation operations
     */
    public static class GenerationResult {
        private final int usersCount;
        private final int tasksGenerated;
        
        public GenerationResult(int usersCount, int tasksGenerated) {
            this.usersCount = usersCount;
            this.tasksGenerated = tasksGenerated;
        }
        
        public int getUsersCount() { return usersCount; }
        public int getTasksGenerated() { return tasksGenerated; }
    }
    
    /**
     * Statistics class for task generation
     */
    public static class TaskGenerationStats {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalTasksGenerated;
        private java.util.Map<LocalDate, Integer> dailyCounts = new java.util.HashMap<>();
        private java.util.Map<Long, Integer> bundleCounts = new java.util.HashMap<>();
        
        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public Integer getTotalTasksGenerated() { return totalTasksGenerated; }
        public void setTotalTasksGenerated(Integer totalTasksGenerated) { this.totalTasksGenerated = totalTasksGenerated; }
        
        public java.util.Map<LocalDate, Integer> getDailyCounts() { return dailyCounts; }
        public void setDailyCounts(java.util.Map<LocalDate, Integer> dailyCounts) { this.dailyCounts = dailyCounts; }
        
        public java.util.Map<Long, Integer> getBundleCounts() { return bundleCounts; }
        public void setBundleCounts(java.util.Map<Long, Integer> bundleCounts) { this.bundleCounts = bundleCounts; }
        
        public void addDailyCount(LocalDate date, Integer count) {
            dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + count);
        }
        
        public void addBundleCount(Long bundleId, Integer count) {
            bundleCounts.put(bundleId, bundleCounts.getOrDefault(bundleId, 0) + count);
        }
        
        public Double getAverageTasksPerDay() {
            if (startDate == null || endDate == null || totalTasksGenerated == null) {
                return 0.0;
            }
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            return daysBetween > 0 ? (double) totalTasksGenerated / daysBetween : 0.0;
        }
    }
}
