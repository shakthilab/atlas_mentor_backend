package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.DisputePriority;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Data
@EqualsAndHashCode(callSuper = true)
public class Dispute extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_approval_id")
    private StudentStatusApproval relatedApproval;
    
    @Column(name = "raised_by", nullable = false)
    private Long raisedBy;
    
    @Column(name = "resolved_by")
    private Long resolvedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status = DisputeStatus.OPEN;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private DisputePriority priority = DisputePriority.MEDIUM;
    
    @Column(name = "dispute_reason", nullable = false, columnDefinition = "TEXT")
    private String disputeReason;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public Dispute() {}
    
    public Dispute(Student student, Long raisedBy, String disputeReason) {
        this.student = student;
        this.raisedBy = raisedBy;
        this.disputeReason = disputeReason;
        this.raisedAt = LocalDateTime.now();
    }
    
    public Dispute(Student student, StudentStatusApproval relatedApproval, Long raisedBy, String disputeReason) {
        this.student = student;
        this.relatedApproval = relatedApproval;
        this.raisedBy = raisedBy;
        this.disputeReason = disputeReason;
        this.raisedAt = LocalDateTime.now();
        // Set resolution deadline based on priority
        setResolutionDeadline();
    }
    
    private void setResolutionDeadline() {
        LocalDateTime now = LocalDateTime.now();
        switch (this.priority) {
            case HIGH:
                this.resolutionDeadline = now.plusDays(3);
                break;
            case MEDIUM:
                this.resolutionDeadline = now.plusDays(7);
                break;
            case LOW:
                this.resolutionDeadline = now.plusDays(14);
                break;
        }
    }
    
    /**
     * Resolve the dispute
     */
    public void resolve(Long resolvedBy, String resolutionNotes) {
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.status = DisputeStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
    
    /**
     * Close the dispute
     */
    public void close(Long resolvedBy, String resolutionNotes) {
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.status = DisputeStatus.CLOSED;
        this.resolvedAt = LocalDateTime.now();
    }
    
    /**
     * Set dispute to in progress
     */
    public void setInProgress() {
        this.status = DisputeStatus.IN_PROGRESS;
    }
    
    /**
     * Soft delete the dispute
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the dispute
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if dispute is deleted
     */
    public boolean isDisputeDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
    
    /**
     * Check if dispute is open
     */
    public boolean isOpen() {
        return DisputeStatus.OPEN.equals(this.status);
    }
    
    /**
     * Check if dispute is in progress
     */
    public boolean isInProgress() {
        return DisputeStatus.IN_PROGRESS.equals(this.status);
    }
    
    /**
     * Check if dispute is resolved
     */
    public boolean isResolved() {
        return DisputeStatus.RESOLVED.equals(this.status);
    }
    
    /**
     * Check if dispute is closed
     */
    public boolean isClosed() {
        return DisputeStatus.CLOSED.equals(this.status);
    }
}
