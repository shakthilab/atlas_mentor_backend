package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.ClientPayoutAction;
import com.lab.atlasmentor.enums.SourceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "client_payouts",
       indexes = {
           @Index(name = "idx_client_payouts_user_id", columnList = "user_id"),
           @Index(name = "idx_client_payouts_student_id", columnList = "student_id"),
           @Index(name = "idx_client_payouts_status", columnList = "payout_status"),
           @Index(name = "idx_client_payouts_source_type", columnList = "source_type"),
           @Index(name = "idx_client_payouts_created_at", columnList = "created_at")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClientPayout extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User user;  // Referral or Company
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
    
    // Amount tracking
    @Column(name = "assigned_amount", precision = 10, scale = 2)
    private BigDecimal assignedAmount;
    
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(name = "balance_amount", insertable = false, updatable = false)
    private BigDecimal balanceAmount;
    
    @Column(name = "settled_amount", precision = 10, scale = 2)
    private BigDecimal settledAmount;
    
    // Status tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false)
    private ClientPayoutStatus payoutStatus = ClientPayoutStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private ClientPayoutStatus previousStatus;
    
    // Dispute tracking
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;
    
    @Column(name = "dispute_response", columnDefinition = "TEXT")
    private String disputeResponse;
    
    @Column(name = "dispute_amount", precision = 10, scale = 2)
    private BigDecimal disputeAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputed_by")
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User disputedBy;
    
    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by")
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User respondedBy;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    // Amount assignment tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User assignedBy;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    // Payment tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_paid_by")
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User lastPaidBy;
    
    @Column(name = "last_paid_at")
    private LocalDateTime lastPaidAt;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;
    
    // Progress tracking
    @Column(name = "payment_progress", insertable = false, updatable = false)
    private BigDecimal paymentProgress;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "clientPayout", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"clientPayout"})
    private List<ClientPayoutActivity> activities;
    
    public ClientPayout() {}
    
    public ClientPayout(User user, Student student, SourceType sourceType) {
        this.user = user;
        this.student = student;
        this.sourceType = sourceType;
        this.payoutStatus = ClientPayoutStatus.PENDING;
    }
    
    // Business logic methods
    public BigDecimal getBalanceAmount() {
        if (assignedAmount != null && paidAmount != null) {
            return assignedAmount.subtract(paidAmount);
        }
        return assignedAmount != null ? assignedAmount : BigDecimal.ZERO;
    }
    
    public BigDecimal getPaymentProgress() {
        if (assignedAmount != null && assignedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return paidAmount.divide(assignedAmount, 2, RoundingMode.HALF_UP)
                          .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
    
    public boolean isFullyPaid() {
        return assignedAmount != null && 
               paidAmount.compareTo(assignedAmount) >= 0;
    }
    
    public boolean isPartiallyPaid() {
        return assignedAmount != null && 
               paidAmount.compareTo(BigDecimal.ZERO) > 0 && 
               paidAmount.compareTo(assignedAmount) < 0;
    }
    
    public boolean isPending() {
        return assignedAmount == null || assignedAmount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public void updateStatusBasedOnPayment() {
        this.previousStatus = this.payoutStatus;
        
        if (assignedAmount == null || assignedAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.payoutStatus = ClientPayoutStatus.PENDING;
        } else if (isFullyPaid()) {
            this.payoutStatus = ClientPayoutStatus.PAID;
        } else if (isPartiallyPaid()) {
            this.payoutStatus = ClientPayoutStatus.PARTIAL_PAID;
        } else {
            this.payoutStatus = ClientPayoutStatus.AMOUNT_ASSIGNED;
        }
    }
    
    public String getPaymentStageDisplay() {
        switch (payoutStatus) {
            case PENDING:
                return "Pending - No amount assigned";
            case AMOUNT_ASSIGNED:
                return String.format("Amount Assigned: ₹%,.2f | Paid: ₹%,.2f | Balance: ₹%,.2f", 
                    assignedAmount, paidAmount, getBalanceAmount());
            case PARTIAL_PAID:
                return String.format("Partial Payment: ₹%,.2f of ₹%,.2f (%.1f%%) | Balance: ₹%,.2f", 
                    paidAmount, assignedAmount, getPaymentProgress(), getBalanceAmount());
            case PAID:
                return String.format("Fully Paid: ₹%,.2f (100%%) | Completed: %s", 
                    assignedAmount, lastPaidAt != null ? lastPaidAt.toLocalDate() : "Unknown");
            case DISPUTE:
                return String.format("Under Dispute: ₹%,.2f | Reason: %s", 
                    disputeAmount != null ? disputeAmount : assignedAmount, 
                    disputeReason != null ? disputeReason.substring(0, Math.min(50, disputeReason.length())) + "..." : "No reason");
            case ACCEPTED:
                return String.format("Dispute Accepted - No Payout | Settled: ₹%,.2f", 
                    settledAmount != null ? settledAmount : BigDecimal.ZERO);
            case REJECTED:
                return String.format("Dispute Rejected - Status Restored: %s", previousStatus);
            default:
                return "Unknown Status";
        }
    }
    
    // Convenience methods
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    public Long getStudentId() {
        return student != null ? student.getId() : null;
    }
    
    public Long getAssignedById() {
        return assignedBy != null ? assignedBy.getId() : null;
    }
    
    public Long getDisputedById() {
        return disputedBy != null ? disputedBy.getId() : null;
    }
    
    public Long getRespondedById() {
        return respondedBy != null ? respondedBy.getId() : null;
    }
    
    public Long getLastPaidById() {
        return lastPaidBy != null ? lastPaidBy.getId() : null;
    }
}
