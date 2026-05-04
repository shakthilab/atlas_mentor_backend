package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DisputePaymentRejectRequest {
    
    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String rejectionReason;
    
    @Size(max = 500, message = "Rejection category must not exceed 500 characters")
    private String rejectionCategory;
    
    private Boolean requiresFurtherAction;
    
    public DisputePaymentRejectRequest() {}
    
    public DisputePaymentRejectRequest(String rejectionReason, String rejectionCategory, Boolean requiresFurtherAction) {
        this.rejectionReason = rejectionReason;
        this.rejectionCategory = rejectionCategory;
        this.requiresFurtherAction = requiresFurtherAction;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public String getRejectionCategory() {
        return rejectionCategory;
    }
    
    public void setRejectionCategory(String rejectionCategory) {
        this.rejectionCategory = rejectionCategory;
    }
    
    public Boolean getRequiresFurtherAction() {
        return requiresFurtherAction;
    }
    
    public void setRequiresFurtherAction(Boolean requiresFurtherAction) {
        this.requiresFurtherAction = requiresFurtherAction;
    }
}
