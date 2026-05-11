package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClientPayoutAmountUpdateRequest {
    
    @NotNull(message = "Client payout ID is required")
    private Long clientPayoutId;
    
    @NotNull(message = "Assigned amount is required")
    @DecimalMin(value = "0.0", message = "Assigned amount must be greater than or equal to 0")
    private BigDecimal assignedAmount;
    
    private String notes;
}
