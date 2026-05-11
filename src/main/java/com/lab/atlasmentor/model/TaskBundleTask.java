package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.Priority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Entity representing individual tasks within a task bundle.
 * Each task in a bundle will be generated as a separate task for users.
 */
@Entity
@Table(name = "task_bundle_tasks",
       indexes = {
           @Index(name = "idx_task_bundle_tasks_bundle_id", columnList = "task_bundle_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskBundleTask extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_bundle_id", nullable = false)
    @JsonIgnoreProperties({"taskBundleTasks", "schedule", "executions"})
    private TaskBundle taskBundle;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;
    
    @Column(name = "task_order")
    private Integer taskOrder = 0;
    
    @Column(name = "default_due_days")
    private Integer defaultDueDays;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public TaskBundleTask() {}
    
    public TaskBundleTask(String title, String description, TaskBundle taskBundle, Priority priority, Integer taskOrder, Integer defaultDueDays) {
        this.title = title;
        this.description = description;
        this.taskBundle = taskBundle;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.taskOrder = taskOrder != null ? taskOrder : 0;
        this.defaultDueDays = defaultDueDays;
        this.isActive = true;
        this.isDeleted = false;
    }
    
    /**
     * Get task bundle ID for backward compatibility
     */
    public Long getTaskBundleId() {
        return taskBundle != null ? taskBundle.getId() : null;
    }
    
    /**
     * Set task bundle by ID for backward compatibility
     */
    public void setTaskBundleId(Long taskBundleId) {
        if (taskBundleId == null) {
            this.taskBundle = null;
        } else {
            this.taskBundle = new TaskBundle();
            this.taskBundle.setId(taskBundleId);
        }
    }
    
    /**
     * Check if task is currently active
     */
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(isDeleted);
    }
    
    /**
     * Calculate due date based on execution date and default due days
     */
    public LocalDate calculateDueDate(LocalDate executionDate) {
        if (executionDate == null || defaultDueDays == null) {
            return null;
        }
        return executionDate.plusDays(defaultDueDays);
    }
}
