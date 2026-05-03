package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.DisputeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisputeResolutionRequest {
    
    @NotNull(message = "Dispute ID is required")
    private Long disputeId;
    
    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;
    
    @NotNull(message = "Resolution status is required")
    private DisputeStatus resolutionStatus;
}
