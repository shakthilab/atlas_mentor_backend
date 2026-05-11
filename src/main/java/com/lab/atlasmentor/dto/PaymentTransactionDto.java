package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentMethod;
import com.lab.atlasmentor.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentTransactionDto {

    private Long id;
    private Long studentId;
    private Long paymentId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private TransactionType transactionType;
    private String transactionReference;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PaymentDisputeActivityDto> disputeActivities;

    public PaymentTransactionDto() {}

    public PaymentTransactionDto(Long id, Long studentId, Long paymentId, BigDecimal amount,
                                 PaymentMethod paymentMethod, TransactionType transactionType,
                                 String transactionReference, String notes, Long createdBy,
                                 String createdByName, LocalDateTime createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.transactionReference = transactionReference;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }
}
