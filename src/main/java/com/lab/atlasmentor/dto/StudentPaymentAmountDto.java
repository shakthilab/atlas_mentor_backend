package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.enums.SourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentPaymentAmountDto {
    
    private Long paymentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private BigDecimal assignedAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private StudentPaymentStatus paymentStatus;
    private SourceType sourceType;
    private Long sourceId;
    private Boolean isAmountLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public StudentPaymentAmountDto() {}
    
    public StudentPaymentAmountDto(Long paymentId, Long studentId, String studentName, String studentEmail,
                                 BigDecimal assignedAmount, BigDecimal paidAmount, BigDecimal remainingAmount,
                                 StudentPaymentStatus paymentStatus, SourceType sourceType, Long sourceId,
                                 Boolean isAmountLocked, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.paymentId = paymentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.assignedAmount = assignedAmount;
        this.paidAmount = paidAmount;
        this.remainingAmount = remainingAmount;
        this.paymentStatus = paymentStatus;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.isAmountLocked = isAmountLocked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
