package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for task bundle task response.
 * Contains individual task information within a bundle.
 */
@Data
public class TaskBundleTaskResponse {
    
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private Integer taskOrder;
    private Integer defaultDueDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    /**
     * Get formatted priority
     */
    public String getFormattedPriority() {
        if (priority != null) {
            return priority.name().charAt(0) + priority.name().substring(1).toLowerCase();
        }
        return null;
    }
    
    /**
     * Get formatted due days text
     */
    public String getFormattedDueDays() {
        if (defaultDueDays == null || defaultDueDays == 0) {
            return "No due date";
        } else if (defaultDueDays == 1) {
            return "Due in 1 day";
        } else {
            return "Due in " + defaultDueDays + " days";
        }
    }
}
