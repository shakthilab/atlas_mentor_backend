package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.FinancialAuditAction;
import com.lab.atlasmentor.model.FinancialAuditLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Read + append repository for FinancialAuditLog.
 *
 * Extends the bare Repository marker interface (NOT JpaRepository / CrudRepository)
 * so that no delete or update methods are inherited. The only mutation exposed is save(),
 * which is used exclusively by FinancialAuditService.
 */
@Component
public interface FinancialAuditLogRepository extends Repository<FinancialAuditLog, Long> {

    <S extends FinancialAuditLog> S save(S entity);

    Optional<FinancialAuditLog> findById(Long id);

    List<FinancialAuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType, Long entityId);

    List<FinancialAuditLog> findByActorIdOrderByOccurredAtDesc(Long actorId);

    List<FinancialAuditLog> findByActionOrderByOccurredAtDesc(FinancialAuditAction action);

    @Query("""
            SELECT l FROM FinancialAuditLog l
            WHERE l.entityType = :entityType
              AND l.entityId   = :entityId
              AND l.occurredAt BETWEEN :from AND :to
            ORDER BY l.occurredAt DESC
           """)
    List<FinancialAuditLog> findByEntityAndDateRange(
            @Param("entityType") String entityType,
            @Param("entityId")   Long entityId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    @Query("""
            SELECT l FROM FinancialAuditLog l
            WHERE l.occurredAt BETWEEN :from AND :to
            ORDER BY l.occurredAt DESC
           """)
    List<FinancialAuditLog> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query(value = "SELECT * FROM financial_audit_log WHERE occurred_at >= :from ORDER BY occurred_at DESC LIMIT 50", nativeQuery = true)
    List<FinancialAuditLog> findRecentLogs(@Param("from") LocalDateTime from);
}
