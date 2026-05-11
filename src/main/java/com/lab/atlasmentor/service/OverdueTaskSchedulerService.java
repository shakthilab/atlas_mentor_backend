package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for automatically marking overdue tasks.
 * Runs periodically to check for tasks that have passed their due date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueTaskSchedulerService {
    
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final UserRepository userRepository;
    
    // System user ID for automatic operations
    private static final Long SYSTEM_USER_ID = 1L;
    
    /**
     * Main scheduler method that runs every hour to check for overdue tasks
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void processOverdueTasks() {
        log.debug("Processing overdue tasks...");
        
        try {
            LocalDate currentDate = LocalDate.now();
            
            // Find tasks that are overdue but not yet marked as overdue
            List<Task> tasksToMarkOverdue = taskRepository.findTasksToMarkOverdue(currentDate);
            
            if (tasksToMarkOverdue.isEmpty()) {
                log.debug("No tasks to mark as overdue");
                return;
            }
            
            log.info("Found {} tasks to mark as overdue", tasksToMarkOverdue.size());
            
            int processedCount = 0;
            for (Task task : tasksToMarkOverdue) {
                try {
                    markTaskAsOverdue(task);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Error marking task {} as overdue: {}", task.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Successfully marked {} tasks as overdue", processedCount);
            
        } catch (Exception e) {
            log.error("Error in overdue task scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Mark a specific task as overdue
     */
    @Transactional
    public void markTaskAsOverdue(Task task) {
        log.debug("Marking task {} as overdue", task.getId());
        
        // Update task status
        task.setStatus(TaskStatus.PENDING);
        task.setUpdatedBy(SYSTEM_USER_ID);
        taskRepository.save(task);
        
        // Create task activity
        createTaskActivity(task, TaskAction.STATUS_CHANGED, SYSTEM_USER_ID);
        
        log.info("Task {} marked as overdue", task.getId());
    }
    
    /**
     * Manually trigger overdue task processing
     */
    @Transactional
    public OverdueTaskResult processOverdueTasksManually() {
        log.info("Manually processing overdue tasks...");
        
        LocalDate currentDate = LocalDate.now();
        List<Task> tasksToMarkOverdue = taskRepository.findTasksToMarkOverdue(currentDate);
        
        int processedCount = 0;
        int errorCount = 0;
        
        for (Task task : tasksToMarkOverdue) {
            try {
                markTaskAsOverdue(task);
                processedCount++;
            } catch (Exception e) {
                log.error("Error marking task {} as overdue: {}", task.getId(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        OverdueTaskResult result = new OverdueTaskResult();
        result.setTotalTasksFound(tasksToMarkOverdue.size());
        result.setTasksProcessed(processedCount);
        result.setTasksWithError(errorCount);
        result.setProcessingTime(LocalDateTime.now());
        
        log.info("Manual overdue task processing completed: {} found, {} processed, {} errors", 
                result.getTotalTasksFound(), result.getTasksProcessed(), result.getTasksWithError());
        
        return result;
    }
    
    /**
     * Get overdue task statistics
     */
    @Transactional(readOnly = true)
    public OverdueTaskStats getOverdueTaskStats() {
        LocalDate currentDate = LocalDate.now();
        
        // Count overdue tasks by status
        long overdueCount = taskRepository.countByDueDateBeforeAndStatusNotAndIsDeletedFalse(
                currentDate, TaskStatus.OVERDUE);
        
        // Count tasks that should be marked overdue
        long shouldMarkOverdueCount = taskRepository.countTasksToMarkOverdue(currentDate);
        
        // Get overdue tasks by priority
        List<Object[]> overdueByPriority = taskRepository.countOverdueTasksByPriority(currentDate);
        
        // Get overdue tasks by age (days overdue)
        List<Object[]> overdueByAge = taskRepository.countOverdueTasksByAge(currentDate);
        
        OverdueTaskStats stats = new OverdueTaskStats();
        stats.setCurrentDate(currentDate);
        stats.setTotalOverdueTasks((int) overdueCount);
        stats.setTasksToMarkOverdue((int) shouldMarkOverdueCount);
        stats.setLastProcessedAt(getLastProcessedTime());
        
        // Process priority breakdown
        for (Object[] row : overdueByPriority) {
            String priority = (String) row[0];
            Long count = (Long) row[1];
            stats.addPriorityCount(priority, count.intValue());
        }
        
        // Process age breakdown
        for (Object[] row : overdueByAge) {
            Integer daysOverdue = (Integer) row[0];
            Long count = (Long) row[1];
            stats.addAgeCount(daysOverdue, count.intValue());
        }
        
        return stats;
    }
    
    /**
     * Get overdue tasks for a specific user
     */
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasksForUser(Long userId) {
        return taskRepository.findByAssignedToIdAndDueDateBeforeAndStatusNotAndIsDeletedFalse(
                userId, LocalDate.now(), TaskStatus.OVERDUE);
    }
    
    /**
     * Get overdue tasks by branch
     */
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasksByBranch(Long branchId) {
        return taskRepository.findByBranchIdAndDueDateBeforeAndStatusNotAndIsDeletedFalse(
                branchId, LocalDate.now(), TaskStatus.OVERDUE);
    }
    
    /**
     * Create task activity record
     */
    private void createTaskActivity(Task task, TaskAction action, Long userId) {
        TaskActivity activity = new TaskActivity();
        activity.setTask(task);
        activity.setAction(action);
        activity.setCreatedBy(userId);
        activity.setUpdatedBy(userId);
        
        // Set doneBy to system user for automated actions
        User systemUser = userRepository.findById(SYSTEM_USER_ID)
                .orElseThrow(() -> new RuntimeException("System user not found"));
        activity.setDoneBy(systemUser);
        
        taskActivityRepository.save(activity);
    }
    
    /**
     * Get last processing time (would typically be stored in a config table)
     */
    private LocalDateTime getLastProcessedTime() {
        // For now, return current time - 1 hour as placeholder
        // In production, this should be stored in a configuration table
        return LocalDateTime.now().minusHours(1);
    }
    
    /**
     * Result class for overdue task processing
     */
    public static class OverdueTaskResult {
        private Integer totalTasksFound;
        private Integer tasksProcessed;
        private Integer tasksWithError;
        private LocalDateTime processingTime;
        
        // Getters and setters
        public Integer getTotalTasksFound() { return totalTasksFound; }
        public void setTotalTasksFound(Integer totalTasksFound) { this.totalTasksFound = totalTasksFound; }
        
        public Integer getTasksProcessed() { return tasksProcessed; }
        public void setTasksProcessed(Integer tasksProcessed) { this.tasksProcessed = tasksProcessed; }
        
        public Integer getTasksWithError() { return tasksWithError; }
        public void setTasksWithError(Integer tasksWithError) { this.tasksWithError = tasksWithError; }
        
        public LocalDateTime getProcessingTime() { return processingTime; }
        public void setProcessingTime(LocalDateTime processingTime) { this.processingTime = processingTime; }
        
        public boolean hasErrors() {
            return tasksWithError != null && tasksWithError > 0;
        }
    }
    
    /**
     * Statistics class for overdue tasks
     */
    public static class OverdueTaskStats {
        private LocalDate currentDate;
        private Integer totalOverdueTasks;
        private Integer tasksToMarkOverdue;
        private LocalDateTime lastProcessedAt;
        private java.util.Map<String, Integer> priorityCounts = new java.util.HashMap<>();
        private java.util.Map<Integer, Integer> ageCounts = new java.util.HashMap<>();
        
        // Getters and setters
        public LocalDate getCurrentDate() { return currentDate; }
        public void setCurrentDate(LocalDate currentDate) { this.currentDate = currentDate; }
        
        public Integer getTotalOverdueTasks() { return totalOverdueTasks; }
        public void setTotalOverdueTasks(Integer totalOverdueTasks) { this.totalOverdueTasks = totalOverdueTasks; }
        
        public Integer getTasksToMarkOverdue() { return tasksToMarkOverdue; }
        public void setTasksToMarkOverdue(Integer tasksToMarkOverdue) { this.tasksToMarkOverdue = tasksToMarkOverdue; }
        
        public LocalDateTime getLastProcessedAt() { return lastProcessedAt; }
        public void setLastProcessedAt(LocalDateTime lastProcessedAt) { this.lastProcessedAt = lastProcessedAt; }
        
        public java.util.Map<String, Integer> getPriorityCounts() { return priorityCounts; }
        public void setPriorityCounts(java.util.Map<String, Integer> priorityCounts) { this.priorityCounts = priorityCounts; }
        
        public java.util.Map<Integer, Integer> getAgeCounts() { return ageCounts; }
        public void setAgeCounts(java.util.Map<Integer, Integer> ageCounts) { this.ageCounts = ageCounts; }
        
        public void addPriorityCount(String priority, Integer count) {
            priorityCounts.put(priority, priorityCounts.getOrDefault(priority, 0) + count);
        }
        
        public void addAgeCount(Integer daysOverdue, Integer count) {
            ageCounts.put(daysOverdue, ageCounts.getOrDefault(daysOverdue, 0) + count);
        }
        
        public Integer getCriticalOverdueTasks() {
            return priorityCounts.getOrDefault("HIGH", 0) + priorityCounts.getOrDefault("CRITICAL", 0);
        }
        
        public Integer getTasksOverdueMoreThan7Days() {
            return ageCounts.entrySet().stream()
                    .filter(entry -> entry.getKey() > 7)
                    .mapToInt(java.util.Map.Entry::getValue)
                    .sum();
        }
    }
}
