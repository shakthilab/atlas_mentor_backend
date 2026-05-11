package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for updating an existing task bundle.
 * Allows partial updates of bundle information.
 */
@Data
public class UpdateTaskBundleRequest {
    
    @Size(max = 200, message = "Bundle name must not exceed 200 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private BundleStatus status;
    
    @Valid
    private UpdateBundleScheduleRequest schedule;
    
    @Valid
    private List<UpdateTaskBundleTaskRequest> tasks;
}
