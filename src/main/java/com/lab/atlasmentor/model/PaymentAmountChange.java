package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_amount_changes")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentAmountChange extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @Column(name = "old_amount", precision = 10, scale = 2)
    private BigDecimal oldAmount;
    
    @Column(name = "new_amount", precision = 10, scale = 2)
    private BigDecimal newAmount;
    
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;
    
    @Column(name = "approved_by")
    private Long approvedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    @Column(name = "approval_remarks", columnDefinition = "TEXT")
    private String approvalRemarks;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public PaymentAmountChange() {}
    
    public PaymentAmountChange(Student student, BigDecimal oldAmount, BigDecimal newAmount, Long requestedBy) {
        this.student = student;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.requestedBy = requestedBy;
    }
    
    /**
     * Approve the amount change request
     */
    public void approve(Long approvedBy, String approvalRemarks) {
        this.status = ApprovalStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvalRemarks = approvalRemarks;
    }
    
    /**
     * Reject the amount change request
     */
    public void reject(Long approvedBy, String approvalRemarks) {
        this.status = ApprovalStatus.REJECTED;
        this.approvedBy = approvedBy;
        this.approvalRemarks = approvalRemarks;
    }
    
    /**
     * Check if the request is pending
     */
    public boolean isPending() {
        return ApprovalStatus.PENDING.equals(this.status);
    }
    
    /**
     * Check if the request is approved
     */
    public boolean isApproved() {
        return ApprovalStatus.APPROVED.equals(this.status);
    }
    
    /**
     * Check if the request is rejected
     */
    public boolean isRejected() {
        return ApprovalStatus.REJECTED.equals(this.status);
    }
    
    /**
     * Soft delete the amount change request
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the amount change request
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if amount change is deleted
     */
    public boolean isAmountChangeDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
