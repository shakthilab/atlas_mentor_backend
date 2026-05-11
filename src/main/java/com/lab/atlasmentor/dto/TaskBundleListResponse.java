package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for task bundle list response.
 * Contains summary information for list views.
 */
@Data
public class TaskBundleListResponse {
    
    private Long id;
    private String name;
    private String description;
    private String roleName;
    private BundleStatus status;
    private String scheduleType;
    private Integer activeTaskCount;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecutionAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    
    /**
     * Check if bundle is currently active
     */
    public boolean isActive() {
        return BundleStatus.ACTIVE.equals(status);
    }
    
    /**
     * Get formatted status
     */
    public String getFormattedStatus() {
        if (status != null) {
            return status.name().charAt(0) + status.name().substring(1).toLowerCase();
        }
        return null;
    }
    
    /**
     * Get formatted schedule type
     */
    public String getFormattedScheduleType() {
        if (scheduleType != null) {
            return scheduleType.charAt(0) + scheduleType.substring(1).toLowerCase();
        }
        return null;
    }
}
