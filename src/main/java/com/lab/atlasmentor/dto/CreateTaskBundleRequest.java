package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for creating a new task bundle.
 * Contains bundle information and initial tasks.
 */
@Data
public class CreateTaskBundleRequest {
    
    @NotBlank(message = "Bundle name is required")
    @Size(max = 200, message = "Bundle name must not exceed 200 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Role ID is required")
    private Long roleId;
    
    @NotNull(message = "Status is required")
    private BundleStatus status;
    
    @Valid
    @NotNull(message = "Schedule configuration is required")
    private CreateBundleScheduleRequest schedule;
    
    @Valid
    private List<CreateTaskBundleTaskRequest> tasks;
}
