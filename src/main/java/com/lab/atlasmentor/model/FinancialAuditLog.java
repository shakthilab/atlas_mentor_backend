package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.FinancialAuditAction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Append-only, tamper-evident audit log for financial operations.
 *
 * Tamper-evidence guarantees:
 *   1. Every column is @Column(updatable = false) — JPA will never issue an UPDATE on this table.
 *   2. @PreUpdate and @PreRemove throw immediately, catching any accidental in-process mutation.
 *   3. integrityHash is a SHA-256 digest of all meaningful fields, computed by FinancialAuditService
 *      before the record is persisted. Re-compute and compare to detect out-of-band DB edits.
 *
 * Creation rules:
 *   - Only FinancialAuditService may construct instances, via the package-visible constructor.
 *   - No soft-delete flag; records are never deleted through the application.
 */
@Entity
@Table(
    name = "financial_audit_log",
    indexes = {
        @Index(name = "idx_fal_entity",    columnList = "entity_type, entity_id"),
        @Index(name = "idx_fal_actor",     columnList = "actor_id"),
        @Index(name = "idx_fal_occurred",  columnList = "occurred_at")
    }
)
public class FinancialAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private FinancialAuditAction action;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private Long actorId;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "remarks", columnDefinition = "TEXT", updatable = false)
    private String remarks;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    /**
     * SHA-256 hex digest of: action|entityType|entityId|actorId|occurredAt|oldValue|newValue
     * Recompute in FinancialAuditService.verifyIntegrity() to detect out-of-band tampering.
     */
    @Column(name = "integrity_hash", nullable = false, updatable = false, length = 64)
    private String integrityHash;

    protected FinancialAuditLog() {}

    private FinancialAuditLog(FinancialAuditAction action, String entityType, Long entityId,
                               Long actorId, String oldValue, String newValue,
                               String remarks, LocalDateTime occurredAt, String integrityHash) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorId = actorId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.remarks = remarks;
        this.occurredAt = occurredAt;
        this.integrityHash = integrityHash;
    }

    /**
     * Factory method — only FinancialAuditService should call this.
     * The integrityHash must be computed by FinancialAuditService before calling.
     */
    public static FinancialAuditLog of(FinancialAuditAction action, String entityType, Long entityId,
                                        Long actorId, String oldValue, String newValue,
                                        String remarks, LocalDateTime occurredAt, String integrityHash) {
        return new FinancialAuditLog(action, entityType, entityId, actorId,
                                     oldValue, newValue, remarks, occurredAt, integrityHash);
    }

    @PreUpdate
    protected void onUpdate() {
        throw new IllegalStateException(
            "FinancialAuditLog records are immutable — UPDATE is not permitted on table financial_audit_log");
    }

    @PreRemove
    protected void onRemove() {
        throw new IllegalStateException(
            "FinancialAuditLog records are immutable — DELETE is not permitted on table financial_audit_log");
    }

    public Long getId()                   { return id; }
    public FinancialAuditAction getAction() { return action; }
    public String getEntityType()         { return entityType; }
    public Long getEntityId()             { return entityId; }
    public Long getActorId()              { return actorId; }
    public String getOldValue()           { return oldValue; }
    public String getNewValue()           { return newValue; }
    public String getRemarks()            { return remarks; }
    public LocalDateTime getOccurredAt()  { return occurredAt; }
    public String getIntegrityHash()      { return integrityHash; }
}
