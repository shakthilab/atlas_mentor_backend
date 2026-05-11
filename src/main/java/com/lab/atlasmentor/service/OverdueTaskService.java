package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskActivity;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service responsible for automatically updating overdue tasks.
 * Runs periodically to check for tasks past their due date and updates their status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueTaskService {
    
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final UserRepository userRepository;
    
    // System user ID for automated status updates
    private static final Long SYSTEM_USER_ID = 1L;
    
    /**
     * Check for overdue tasks every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void updateOverdueTasks() {
        log.debug("Checking for overdue tasks...");
        
        try {
            LocalDate currentDate = LocalDate.now();
            
            // Find tasks that are overdue (due date passed and not completed)
            List<Task> overdueTasks = taskRepository.findOverdueTasks(currentDate);
            
            if (overdueTasks.isEmpty()) {
                log.debug("No overdue tasks found");
                return;
            }
            
            log.info("Found {} overdue tasks to update", overdueTasks.size());
            
            int updatedCount = 0;
            for (Task task : overdueTasks) {
                try {
                    updateTaskToOverdue(task);
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Error updating task {} to overdue: {}", task.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Successfully updated {} tasks to OVERDUE status", updatedCount);
            
        } catch (Exception e) {
            log.error("Error in overdue task scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update a single task to OVERDUE status
     */
    @Transactional
    public void updateTaskToOverdue(Task task) {
        // Only update if Task is not already completed or overdue
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.OVERDUE) {
            return;
        }
        
        TaskStatus previousStatus = task.getStatus();
        
        try {
            task.setStatus(TaskStatus.OVERDUE);
            task.setUpdatedBy(SYSTEM_USER_ID);
            
            taskRepository.save(task);
            
            // Create activity record for status change
            createTaskActivity(task, "STATUS_CHANGED", SYSTEM_USER_ID, 
                           "Status automatically changed from " + previousStatus + " to OVERDUE due to passed due date");
            
            log.debug("Task {} updated to OVERDUE status (was: {})", task.getId(), previousStatus);
            
        } catch (Exception e) {
            log.error("Error updating task {} to OVERDUE status: {}", task.getId(), e.getMessage());
            // Revert status to avoid constraint violation
            task.setStatus(previousStatus);
            task.setUpdatedBy(SYSTEM_USER_ID);
            taskRepository.save(task);
            throw e;
        }
    }
    
    /**
     * Manually trigger overdue task check
     */
    @Transactional
    public int manuallyUpdateOverdueTasks() {
        LocalDate currentDate = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(currentDate);
        
        int updatedCount = 0;
        for (Task task : overdueTasks) {
            try {
                updateTaskToOverdue(task);
                updatedCount++;
            } catch (Exception e) {
                log.error("Error updating task {} to overdue: {}", task.getId(), e.getMessage(), e);
            }
        }
        
        return updatedCount;
    }
    
    /**
     * Create task activity record
     */
    private void createTaskActivity(Task task, String action, Long userId, String comment) {
        TaskActivity activity = new TaskActivity();
        activity.setTask(task);
        activity.setAction(com.lab.atlasmentor.enums.TaskAction.valueOf(action));
        activity.setOldValue("Status: " + task.getStatus());
        activity.setNewValue("Status: OVERDUE - " + comment);
        activity.setDoneBy(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId)));
        
        taskActivityRepository.save(activity);
    }
    
    /**
     * Get overdue tasks statistics
     */
    @Transactional(readOnly = true)
    public OverdueTaskStats getOverdueTaskStats() {
        LocalDate currentDate = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(currentDate);
        
        OverdueTaskStats stats = new OverdueTaskStats();
        stats.setTotalOverdueTasks(overdueTasks.size());
        stats.setCheckDate(currentDate);
        
        // Count by priority
        stats.setHighPriorityOverdue((int) overdueTasks.stream()
                .filter(task -> task.getPriority() == com.lab.atlasmentor.enums.Priority.HIGH)
                .count());
        
        stats.setMediumPriorityOverdue((int) overdueTasks.stream()
                .filter(task -> task.getPriority() == com.lab.atlasmentor.enums.Priority.MEDIUM)
                .count());
        
        stats.setLowPriorityOverdue((int) overdueTasks.stream()
                .filter(task -> task.getPriority() == com.lab.atlasmentor.enums.Priority.LOW)
                .count());
        
        return stats;
    }
    
    /**
     * DTO for overdue task statistics
     */
    public static class OverdueTaskStats {
        private Integer totalOverdueTasks;
        private Integer highPriorityOverdue;
        private Integer mediumPriorityOverdue;
        private Integer lowPriorityOverdue;
        private LocalDate checkDate;
        
        // Getters and setters
        public Integer getTotalOverdueTasks() { return totalOverdueTasks; }
        public void setTotalOverdueTasks(Integer totalOverdueTasks) { this.totalOverdueTasks = totalOverdueTasks; }
        
        public Integer getHighPriorityOverdue() { return highPriorityOverdue; }
        public void setHighPriorityOverdue(Integer highPriorityOverdue) { this.highPriorityOverdue = highPriorityOverdue; }
        
        public Integer getMediumPriorityOverdue() { return mediumPriorityOverdue; }
        public void setMediumPriorityOverdue(Integer mediumPriorityOverdue) { this.mediumPriorityOverdue = mediumPriorityOverdue; }
        
        public Integer getLowPriorityOverdue() { return lowPriorityOverdue; }
        public void setLowPriorityOverdue(Integer lowPriorityOverdue) { this.lowPriorityOverdue = lowPriorityOverdue; }
        
        public LocalDate getCheckDate() { return checkDate; }
        public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }
    }
}
