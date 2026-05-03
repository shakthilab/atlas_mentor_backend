package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.DisputePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisputeRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    private Long relatedApprovalId;
    
    @NotBlank(message = "Dispute reason is required")
    private String disputeReason;
    
    @NotNull(message = "Priority is required")
    private DisputePriority priority;
}
