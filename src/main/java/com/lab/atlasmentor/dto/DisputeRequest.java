package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;

public class DisputeRequest {
    @NotBlank(message = "Dispute reason is required")
    private String reason;
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
