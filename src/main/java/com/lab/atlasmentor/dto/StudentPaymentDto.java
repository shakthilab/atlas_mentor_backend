package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentPaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentPaymentDto {
    
    private Long id;
    private Long studentId;
    private BigDecimal assignedAmount;
    private BigDecimal paidAmount;
    private StudentPaymentStatus paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public StudentPaymentDto() {}
    
    public StudentPaymentDto(Long id, Long studentId, BigDecimal assignedAmount, BigDecimal paidAmount,
                           StudentPaymentStatus paymentStatus, String notes, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.assignedAmount = assignedAmount;
        this.paidAmount = paidAmount;
        this.paymentStatus = paymentStatus;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
