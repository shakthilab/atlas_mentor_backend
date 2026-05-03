package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentPaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentPaymentStatusUpdateRequest {
    
    @NotNull(message = "Payment ID is required")
    private Long paymentId;
    
    @NotNull(message = "Payment status is required")
    private StudentPaymentStatus paymentStatus;
    
    private String notes;
}
