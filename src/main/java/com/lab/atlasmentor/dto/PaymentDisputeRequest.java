package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentDisputeRequest {
    
    @NotBlank(message = "Dispute reason is required")
    @Size(max = 1000, message = "Dispute reason must not exceed 1000 characters")
    private String disputeReason;
    
    public PaymentDisputeRequest() {}
    
    public PaymentDisputeRequest(String disputeReason) {
        this.disputeReason = disputeReason;
    }
    
    public String getDisputeReason() {
        return disputeReason;
    }
    
    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }
}
