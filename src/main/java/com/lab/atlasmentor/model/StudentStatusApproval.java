package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.ApprovalStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "student_status_approvals")
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentStatusApproval extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false)
    private StudentStatusEnhanced requestedStatus;
    
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
    
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "proof_url", length = 500)
    private String proofUrl;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public StudentStatusApproval() {}
    
    public StudentStatusApproval(Student student, StudentStatusEnhanced requestedStatus, Long requestedBy) {
        this.student = student;
        this.requestedStatus = requestedStatus;
        this.requestedBy = requestedBy;
    }
    
    /**
     * Approve the status change request
     */
    public void approve(Long approvedBy, String approvalRemarks) {
        this.status = ApprovalStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvalRemarks = approvalRemarks;
    }
    
    /**
     * Reject the status change request
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
     * Soft delete the approval request
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the approval request
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if approval is deleted
     */
    public boolean isApprovalDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
