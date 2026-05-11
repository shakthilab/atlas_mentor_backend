package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a task within a task bundle.
 * Contains individual task configuration.
 */
@Data
public class CreateTaskBundleTaskRequest {
    
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Task description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Priority is required")
    private Priority priority;
    
    private Integer taskOrder;
    
    private Integer defaultDueDays; // Days from execution date when task becomes due
}
