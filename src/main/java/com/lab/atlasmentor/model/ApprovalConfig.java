package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.ApprovalActionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "approval_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalConfig extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, unique = true)
    private ApprovalActionType actionType;
    
    @Column(name = "min_approvals", nullable = false)
    private Integer minApprovals = 1;
    
    @Column(name = "require_different_roles", nullable = false)
    private Boolean requireDifferentRoles = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public ApprovalConfig() {}
    
    public ApprovalConfig(ApprovalActionType actionType, Integer minApprovals, Boolean requireDifferentRoles) {
        this.actionType = actionType;
        this.minApprovals = minApprovals;
        this.requireDifferentRoles = requireDifferentRoles;
    }
    
    /**
     * Soft delete the config
     */
    public void softDelete() {
        this.isDeleted = true;
        this.isActive = false;
    }
    
    /**
     * Restore the config
     */
    public void restore() {
        this.isDeleted = false;
        this.isActive = true;
    }
    
    /**
     * Check if config is deleted
     */
    public boolean isConfigDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
    
    /**
     * Activate the config
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * Deactivate the config
     */
    public void deactivate() {
        this.isActive = false;
    }
}
