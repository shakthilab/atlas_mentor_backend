package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentDisputeRequest {

    @NotBlank(message = "Dispute reason is required")
    private String disputeReason;
}
