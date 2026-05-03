package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.PaymentMethod;
import com.lab.atlasmentor.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentTransactionWithDisputeDto {
    
    private Long transactionId;
    private Long studentId;
    private Long paymentId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private TransactionType transactionType;
    private String transactionReference;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Dispute information
    private DisputeStatus disputeStatus;
    
    public PaymentTransactionWithDisputeDto() {}
    
    public PaymentTransactionWithDisputeDto(Long transactionId, Long studentId, Long paymentId, 
                                          BigDecimal amount, PaymentMethod paymentMethod, 
                                          TransactionType transactionType, String transactionReference, 
                                          String notes, LocalDateTime createdAt, LocalDateTime updatedAt,
                                          DisputeStatus disputeStatus) {
        this.transactionId = transactionId;
        this.studentId = studentId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.transactionReference = transactionReference;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.disputeStatus = disputeStatus;
    }
}
