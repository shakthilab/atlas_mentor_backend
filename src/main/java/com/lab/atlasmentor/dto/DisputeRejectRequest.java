package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeRejectRequest {
    
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
    
    public DisputeRejectRequest() {}
    
    public DisputeRejectRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
