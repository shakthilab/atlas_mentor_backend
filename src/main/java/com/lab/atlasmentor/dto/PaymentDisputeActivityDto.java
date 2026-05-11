package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentDisputeAction;
import com.lab.atlasmentor.enums.DisputeStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDisputeActivityDto {
    
    private Long id;
    private Long paymentId;
    private PaymentDisputeAction action;
    private String oldValue;
    private String newValue;
    private String reason;
    private UserInfoDto doneBy;
    private LocalDateTime doneAt;
    private LocalDateTime updatedAt;
    private DisputeStatus status;

    public PaymentDisputeActivityDto() {}

    public PaymentDisputeActivityDto(Long id, Long paymentId, PaymentDisputeAction action,
                                   String oldValue, String newValue, String reason,
                                   UserInfoDto doneBy, LocalDateTime doneAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.doneBy = doneBy;
        this.doneAt = doneAt;
    }
}
