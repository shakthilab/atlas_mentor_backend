package com.lab.atlasmentor.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for bundle execution response.
 * Contains execution history and statistics.
 */
@Data
public class BundleExecutionResponse {
    
    private Long id;
    private TaskBundleResponse taskBundle;
    private LocalDateTime executionDate;
    private Integer usersCount;
    private Integer tasksGenerated;
    private String executionStatus;
    private String errorMessage;
    private Long executionDurationMs;
    private LocalDateTime nextExecutionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    /**
     * Get formatted execution status
     */
    public String getFormattedExecutionStatus() {
        if (executionStatus != null) {
            return executionStatus.charAt(0) + executionStatus.substring(1).toLowerCase();
        }
        return null;
    }
    
    /**
     * Get formatted execution duration
     */
    public String getFormattedExecutionDuration() {
        if (executionDurationMs == null) {
            return "N/A";
        }
        
        long seconds = executionDurationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
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
     * Get success rate percentage
     */
    public Double getSuccessRate() {
        if (usersCount == null || usersCount == 0) {
            return 0.0;
        }
        if (tasksGenerated == null) {
            return 0.0;
        }
        
        // Assuming each user should get tasks from all active bundle tasks
        // This is a simplified calculation - actual logic may vary
        return (double) tasksGenerated / (usersCount * 1) * 100;
    }
}
