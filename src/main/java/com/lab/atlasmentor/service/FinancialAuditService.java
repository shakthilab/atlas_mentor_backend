package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.FinancialAuditAction;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.FinancialAuditLog;
import com.lab.atlasmentor.repository.FinancialAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Single write entry-point for the tamper-evident financial audit log.
 *
 * Every financial operation (payment creation, amount assignment, approval/rejection,
 * payout changes, disputes) MUST call record() within the same transaction as the
 * operation itself. If the audit write fails, the enclosing transaction rolls back —
 * we never allow a financial mutation to succeed without a corresponding audit entry.
 */
@Slf4j
@Service
public class FinancialAuditService {

    @Autowired
    private FinancialAuditLogRepository repository;

    /**
     * Appends one tamper-evident audit entry.
     *
     * Runs in the caller's transaction (REQUIRED). A failure here propagates up and
     * rolls back the enclosing financial operation.
     *
     * @param action      what happened
     * @param entityType  logical entity name, e.g. "StudentPayment", "ClientPayout"
     * @param entityId    primary key of the affected entity
     * @param actorId     user ID who performed the action
     * @param oldValue    human-readable previous state (may be null)
     * @param newValue    human-readable new state (may be null)
     * @param remarks     optional free-text note from the caller
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public FinancialAuditLog record(FinancialAuditAction action,
                                    String entityType,
                                    Long entityId,
                                    Long actorId,
                                    String oldValue,
                                    String newValue,
                                    String remarks) {
        LocalDateTime now = LocalDateTime.now();
        String hash = computeIntegrityHash(action, entityType, entityId, actorId, now, oldValue, newValue);
        FinancialAuditLog entry = FinancialAuditLog.of(
                action, entityType, entityId, actorId, oldValue, newValue, remarks, now, hash);
        return repository.save(entry);
    }

    /**
     * Verifies that a stored record's integrityHash matches a freshly computed hash.
     * Returns false if the record was modified out-of-band (e.g., direct SQL UPDATE).
     */
    public boolean verifyIntegrity(Long auditLogId) {
        Optional<FinancialAuditLog> opt = repository.findById(auditLogId);
        if (opt.isEmpty()) {
            return false;
        }
        FinancialAuditLog entry = opt.get();
        String expected = computeIntegrityHash(
                entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                entry.getActorId(), entry.getOccurredAt(),
                entry.getOldValue(), entry.getNewValue());
        return expected.equals(entry.getIntegrityHash());
    }

    /**
     * Bulk-verify all records for an entity. Returns a list of IDs whose hash does not match.
     */
    public List<Long> findTamperedRecords(String entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId)
                .stream()
                .filter(e -> {
                    String expected = computeIntegrityHash(
                            e.getAction(), e.getEntityType(), e.getEntityId(),
                            e.getActorId(), e.getOccurredAt(),
                            e.getOldValue(), e.getNewValue());
                    return !expected.equals(e.getIntegrityHash());
                })
                .map(FinancialAuditLog::getId)
                .toList();
    }

    // ==================== READ METHODS ====================

    public List<FinancialAuditLog> getByEntity(String entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId);
    }

    public List<FinancialAuditLog> getByActor(Long actorId) {
        return repository.findByActorIdOrderByOccurredAtDesc(actorId);
    }

    public List<FinancialAuditLog> getByAction(FinancialAuditAction action) {
        return repository.findByActionOrderByOccurredAtDesc(action);
    }

    public List<FinancialAuditLog> getByDateRange(LocalDateTime from, LocalDateTime to) {
        return repository.findByDateRange(from, to);
    }

    // ==================== HASH COMPUTATION ====================

    private String computeIntegrityHash(FinancialAuditAction action, String entityType,
                                        Long entityId, Long actorId, LocalDateTime occurredAt,
                                        String oldValue, String newValue) {
        String raw = String.join("|",
                String.valueOf(action),
                String.valueOf(entityType),
                String.valueOf(entityId),
                String.valueOf(actorId),
                String.valueOf(occurredAt),
                oldValue  != null ? oldValue  : "",
                newValue  != null ? newValue  : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("SHA-256 unavailable", e);
        }
    }
}
