package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ClientPayoutAction;
import com.lab.atlasmentor.enums.DisputeStage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientPayoutActivityDto {
    private Long id;
    private Long clientPayoutId;
    private ClientPayoutAction action;
    private String oldValue;
    private String newValue;
    private BigDecimal oldAmount;
    private BigDecimal newAmount;
    private String reason;
    private UserInfoDto doneBy;
    private LocalDateTime doneAt;
    private String paymentMethod;
    private String transactionReference;
    private DisputeStage disputeStage;
    private String previousStatus;
    private String newStatus;
    
    // Constructors
    public ClientPayoutActivityDto() {}
    
    public ClientPayoutActivityDto(Long id, Long clientPayoutId, ClientPayoutAction action,
                                 String oldValue, String newValue, String reason, 
                                 UserInfoDto doneBy, LocalDateTime doneAt) {
        this.id = id;
        this.clientPayoutId = clientPayoutId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.doneBy = doneBy;
        this.doneAt = doneAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getClientPayoutId() { return clientPayoutId; }
    public void setClientPayoutId(Long clientPayoutId) { this.clientPayoutId = clientPayoutId; }
    
    public ClientPayoutAction getAction() { return action; }
    public void setAction(ClientPayoutAction action) { this.action = action; }
    
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    
    public BigDecimal getOldAmount() { return oldAmount; }
    public void setOldAmount(BigDecimal oldAmount) { this.oldAmount = oldAmount; }
    
    public BigDecimal getNewAmount() { return newAmount; }
    public void setNewAmount(BigDecimal newAmount) { this.newAmount = newAmount; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public UserInfoDto getDoneBy() { return doneBy; }
    public void setDoneBy(UserInfoDto doneBy) { this.doneBy = doneBy; }
    
    public LocalDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(LocalDateTime doneAt) { this.doneAt = doneAt; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    
    public DisputeStage getDisputeStage() { return disputeStage; }
    public void setDisputeStage(DisputeStage disputeStage) { this.disputeStage = disputeStage; }
    
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
}
