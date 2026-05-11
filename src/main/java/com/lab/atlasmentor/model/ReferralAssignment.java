package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "referral_assignments",
       indexes = {
           @Index(name = "idx_referral_assignments_referral_id", columnList = "referral_id"),
           @Index(name = "idx_referral_assignments_assigned_to_id", columnList = "assigned_to_id")
       })
@Data
@EqualsAndHashCode(callSuper = false)
public class ReferralAssignment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id", nullable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User referral;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", nullable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User assignedTo;
    
    public ReferralAssignment() {}
    
    public ReferralAssignment(User referral, User assignedTo) {
        this.referral = referral;
        this.assignedTo = assignedTo;
    }
    
    // Convenience methods
    public Long getReferralId() {
        return referral != null ? referral.getId() : null;
    }
    
    public void setReferralId(Long referralId) {
        if (referralId != null) {
            this.referral = new User();
            this.referral.setId(referralId);
        }
    }
    
    public Long getAssignedToId() {
        return assignedTo != null ? assignedTo.getId() : null;
    }
    
    public void setAssignedToId(Long assignedToId) {
        if (assignedToId != null) {
            this.assignedTo = new User();
            this.assignedTo.setId(assignedToId);
        }
    }
}
