package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentStatus;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class PaymentUpdateRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private PaymentStatus status;
    
    private String notes;
    
    public PaymentUpdateRequest() {}
    
    public PaymentUpdateRequest(Long studentId, BigDecimal amount, PaymentStatus status, String notes) {
        this.studentId = studentId;
        this.amount = amount;
        this.status = status;
        this.notes = notes;
    }
}
