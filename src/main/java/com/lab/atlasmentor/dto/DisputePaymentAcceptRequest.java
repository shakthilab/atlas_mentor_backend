package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DisputePaymentAcceptRequest {
    
    @NotBlank(message = "Acceptance notes are required")
    @Size(max = 1000, message = "Acceptance notes must not exceed 1000 characters")
    private String acceptanceNotes;
    
    @Size(max = 500, message = "Payment reference must not exceed 500 characters")
    private String paymentReference;
    
    private Double acceptedAmount;
    
    public DisputePaymentAcceptRequest() {}
    
    public DisputePaymentAcceptRequest(String acceptanceNotes, String paymentReference, Double acceptedAmount) {
        this.acceptanceNotes = acceptanceNotes;
        this.paymentReference = paymentReference;
        this.acceptedAmount = acceptedAmount;
    }
    
    public String getAcceptanceNotes() {
        return acceptanceNotes;
    }
    
    public void setAcceptanceNotes(String acceptanceNotes) {
        this.acceptanceNotes = acceptanceNotes;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }
    
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    
    public Double getAcceptedAmount() {
        return acceptedAmount;
    }
    
    public void setAcceptedAmount(Double acceptedAmount) {
        this.acceptedAmount = acceptedAmount;
    }
}
