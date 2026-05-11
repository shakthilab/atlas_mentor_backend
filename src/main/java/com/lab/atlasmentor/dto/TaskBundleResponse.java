package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for task bundle response.
 * Contains complete bundle information with tasks and schedule.
 */
@Data
public class TaskBundleResponse {
    
    private Long id;
    private String name;
    private String description;
    private RoleResponse role;
    private BundleStatus status;
    private BundleScheduleResponse schedule;
    private List<TaskBundleTaskResponse> tasks;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecutionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    /**
     * Get active task count
     */
    public Integer getActiveTaskCount() {
        if (tasks == null) return 0;
        return (int) tasks.stream()
                .filter(task -> Boolean.TRUE.equals(task.getIsActive()))
                .count();
    }
    
    /**
     * Check if bundle is currently active
     */
    public boolean isActive() {
        return BundleStatus.ACTIVE.equals(status);
    }
}
