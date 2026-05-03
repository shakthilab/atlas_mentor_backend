package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentStatus;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class PaymentCreateRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Referral ID is required")
    private Long referralId;
    
    @NotNull(message = "Company ID is required")
    private Long companyId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private Long branchId;
    
    private String notes;
    
    public PaymentCreateRequest() {}
    
    public PaymentCreateRequest(Long studentId, Long referralId, Long companyId, BigDecimal amount, Long branchId, String notes) {
        this.studentId = studentId;
        this.referralId = referralId;
        this.companyId = companyId;
        this.amount = amount;
        this.branchId = branchId;
        this.notes = notes;
    }
}
