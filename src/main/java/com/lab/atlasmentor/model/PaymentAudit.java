package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.PaymentAuditAction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "payment_audit")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentAudit extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private PaymentAuditAction action;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "done_by", nullable = false)
    private Long doneBy;
    
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    @Column(name = "entity_name", length = 100)
    private String entityName;
    
    @Column(name = "old_value_json", columnDefinition = "TEXT")
    private String oldValueJson;
    
    @Column(name = "new_value_json", columnDefinition = "TEXT")
    private String newValueJson;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public PaymentAudit() {}
    
    public PaymentAudit(Student student, PaymentAuditAction action, String oldValue, String newValue, Long doneBy) {
        this.student = student;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.doneBy = doneBy;
    }
    
    public PaymentAudit(Student student, PaymentAuditAction action, String oldValue, String newValue, Long doneBy, String remarks) {
        this.student = student;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.doneBy = doneBy;
        this.remarks = remarks;
    }
    
    public PaymentAudit(Student student, PaymentAuditAction action, String oldValue, String newValue, 
                       String oldValueJson, String newValueJson, String entityName, Long doneBy, String requestId, String remarks) {
        this.student = student;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldValueJson = oldValueJson;
        this.newValueJson = newValueJson;
        this.entityName = entityName;
        this.doneBy = doneBy;
        this.requestId = requestId;
        this.remarks = remarks;
    }
    
    /**
     * Soft delete the audit record
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the audit record
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if audit is deleted
     */
    public boolean isAuditDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
