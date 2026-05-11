package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Entity tracking execution history of task bundles.
 * Used to prevent duplicate execution and provide audit trails.
 */
@Entity
@Table(name = "bundle_executions")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BundleExecution extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_bundle_id", nullable = false)
    @JsonIgnoreProperties({"executions", "taskBundleTasks", "schedule"})
    private TaskBundle taskBundle;
    
    @Column(name = "execution_date", nullable = false)
    private LocalDateTime executionDate;
    
    @Column(name = "users_count", nullable = false)
    private Integer usersCount = 0;
    
    @Column(name = "tasks_generated", nullable = false)
    private Integer tasksGenerated = 0;
    
    @Column(name = "execution_status", nullable = false, length = 20)
    private String executionStatus; // SUCCESS, FAILED, PARTIAL
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;
    
    @Column(name = "next_execution_date")
    private LocalDateTime nextExecutionDate;
    
    public BundleExecution() {}
    
    public BundleExecution(TaskBundle taskBundle, LocalDateTime executionDate, String executionStatus) {
        this.taskBundle = taskBundle;
        this.executionDate = executionDate;
        this.executionStatus = executionStatus;
        this.usersCount = 0;
        this.tasksGenerated = 0;
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
     * Check if execution was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(executionStatus);
    }
    
    /**
     * Check if execution failed
     */
    public boolean isFailed() {
        return "FAILED".equals(executionStatus);
    }
    
    /**
     * Check if execution was partial
     */
    public boolean isPartial() {
        return "PARTIAL".equals(executionStatus);
    }
}
