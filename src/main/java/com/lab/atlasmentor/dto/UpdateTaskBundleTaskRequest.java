package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating a task within a task bundle.
 * Allows partial updates of task configuration.
 */
@Data
public class UpdateTaskBundleTaskRequest {
    
    private Long id; // Required to identify the task to update
    
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Task description must not exceed 1000 characters")
    private String description;
    
    private Priority priority;
    
    private Integer taskOrder;
    
    private Integer defaultDueDays; // Days from execution date when task becomes due
    
    private Boolean isActive;
}
