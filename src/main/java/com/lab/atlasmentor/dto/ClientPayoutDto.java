package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ClientPayoutDto {
    private Long id;
    private UserInfoDto user; // Referral/Company
    private Long studentId;
    private String studentName;
    private SourceType sourceType;
    private BigDecimal assignedAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private BigDecimal settledAmount;
    private ClientPayoutStatus payoutStatus;
    private ClientPayoutStatus previousStatus;
    private BigDecimal paymentProgress;
    private String paymentStageDisplay;
    
    // Dispute tracking
    private String disputeReason;
    private String disputeResponse;
    private BigDecimal disputeAmount;
    private LocalDateTime disputedAt;
    private LocalDateTime respondedAt;
    
    // User tracking
    private UserInfoDto assignedBy;
    private UserInfoDto disputedBy;
    private UserInfoDto respondedBy;
    private UserInfoDto lastPaidBy;
    
    // Timeline tracking
    private LocalDateTime assignedAt;
    private LocalDateTime lastPaidAt;
    
    // Payment tracking
    private String paymentMethod;
    private String transactionReference;
    
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Activity history
    private List<ClientPayoutActivityDto> activities;
    
    // Constructors
    public ClientPayoutDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public UserInfoDto getUser() { return user; }
    public void setUser(UserInfoDto user) { this.user = user; }
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    
    public BigDecimal getAssignedAmount() { return assignedAmount; }
    public void setAssignedAmount(BigDecimal assignedAmount) { this.assignedAmount = assignedAmount; }
    
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
    
    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
    
    public ClientPayoutStatus getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(ClientPayoutStatus payoutStatus) { this.payoutStatus = payoutStatus; }
    
    public ClientPayoutStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ClientPayoutStatus previousStatus) { this.previousStatus = previousStatus; }
    
    public BigDecimal getPaymentProgress() { return paymentProgress; }
    public void setPaymentProgress(BigDecimal paymentProgress) { this.paymentProgress = paymentProgress; }
    
    public String getPaymentStageDisplay() { return paymentStageDisplay; }
    public void setPaymentStageDisplay(String paymentStageDisplay) { this.paymentStageDisplay = paymentStageDisplay; }
    
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }
    
    public String getDisputeResponse() { return disputeResponse; }
    public void setDisputeResponse(String disputeResponse) { this.disputeResponse = disputeResponse; }
    
    public BigDecimal getDisputeAmount() { return disputeAmount; }
    public void setDisputeAmount(BigDecimal disputeAmount) { this.disputeAmount = disputeAmount; }
    
    public LocalDateTime getDisputedAt() { return disputedAt; }
    public void setDisputedAt(LocalDateTime disputedAt) { this.disputedAt = disputedAt; }
    
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    
    public UserInfoDto getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UserInfoDto assignedBy) { this.assignedBy = assignedBy; }
    
    public UserInfoDto getDisputedBy() { return disputedBy; }
    public void setDisputedBy(UserInfoDto disputedBy) { this.disputedBy = disputedBy; }
    
    public UserInfoDto getRespondedBy() { return respondedBy; }
    public void setRespondedBy(UserInfoDto respondedBy) { this.respondedBy = respondedBy; }
    
    public UserInfoDto getLastPaidBy() { return lastPaidBy; }
    public void setLastPaidBy(UserInfoDto lastPaidBy) { this.lastPaidBy = lastPaidBy; }
    
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    
    public LocalDateTime getLastPaidAt() { return lastPaidAt; }
    public void setLastPaidAt(LocalDateTime lastPaidAt) { this.lastPaidAt = lastPaidAt; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<ClientPayoutActivityDto> getActivities() { return activities; }
    public void setActivities(List<ClientPayoutActivityDto> activities) { this.activities = activities; }
}
