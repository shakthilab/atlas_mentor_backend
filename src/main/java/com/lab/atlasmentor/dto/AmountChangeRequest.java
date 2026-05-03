package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountChangeRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "New amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have maximum 8 integer digits and 2 fraction digits")
    private BigDecimal newAmount;
    
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}
