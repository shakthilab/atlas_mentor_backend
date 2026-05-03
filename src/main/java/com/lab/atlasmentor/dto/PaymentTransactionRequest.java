package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentMethod;
import com.lab.atlasmentor.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentTransactionRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have maximum 8 integer digits and 2 fraction digits")
    private BigDecimal amount;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @Size(max = 200, message = "Transaction reference must not exceed 200 characters")
    private String transactionReference;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
