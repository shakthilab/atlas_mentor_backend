package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.enums.BundleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TaskBundle entity operations.
 * Provides role-based and status-based queries for bundle management.
 */
@Repository
public interface TaskBundleRepository extends JpaRepository<TaskBundle, Long> {
    
    /**
     * Find active task bundles by role
     */


    @Query("SELECT tb FROM TaskBundle tb WHERE tb.role.id = :roleId AND tb.status = :status AND tb.isDeleted = false")
    List<TaskBundle> findActiveByRole(@Param("roleId") Long roleId, @Param("status") BundleStatus status);
    
    /**
     * Find all active task bundles ready for execution
     */
    @Query("SELECT tb FROM TaskBundle tb WHERE tb.status = 'ACTIVE' AND tb.nextExecutionAt <= :currentTime AND tb.isDeleted = false")
    List<TaskBundle> findReadyForExecution(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find task bundles by role and status
     */
    List<TaskBundle> findByRoleIdAndStatus(Long roleId, BundleStatus status);
    
    /**
     * Find all task bundles by status
     */
    List<TaskBundle> findByStatus(BundleStatus status);
    
    /**
     * Find task bundle by ID with role and schedule eagerly loaded.
     * Use this for execution paths that cross REQUIRES_NEW transaction boundaries
     * to prevent LazyInitializationException.
     */
    @Query("SELECT tb FROM TaskBundle tb LEFT JOIN FETCH tb.role LEFT JOIN FETCH tb.schedule WHERE tb.id = :id")
    Optional<TaskBundle> findByIdWithDetails(@Param("id") Long id);

    /**
     * Find task bundle by name and role (for uniqueness validation)
     */
    @Query("SELECT tb FROM TaskBundle tb WHERE tb.name = :name AND tb.role.id = :roleId AND tb.isDeleted = false")
    Optional<TaskBundle> findByNameAndRoleId(@Param("name") String name, @Param("roleId") Long roleId);
    
    /**
     * Find task bundles by multiple roles
     */
    @Query("SELECT tb FROM TaskBundle tb WHERE tb.role.id IN :roleIds AND tb.status = :status AND tb.isDeleted = false")
    List<TaskBundle> findByRoleIdsAndStatus(@Param("roleIds") List<Long> roleIds, @Param("status") BundleStatus status);
    
    /**
     * Count active task bundles by role
     */
    @Query("SELECT COUNT(tb) FROM TaskBundle tb WHERE tb.role.id = :roleId AND tb.status = :status AND tb.isDeleted = false")
    Long countActiveByRole(@Param("roleId") Long roleId, @Param("status") BundleStatus status);
    
    /**
     * Find task bundles that need next execution date calculation
     */
    @Query("SELECT tb FROM TaskBundle tb WHERE tb.status = 'ACTIVE' AND tb.isDeleted = false AND (tb.nextExecutionAt IS NULL OR tb.nextExecutionAt <= :currentTime)")
    List<TaskBundle> findNeedingNextExecutionUpdate(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find task bundles by creator
     */
    List<TaskBundle> findByCreatedBy(Long createdBy);
    
    /**
     * Search task bundles by name or description
     */
    @Query("SELECT tb FROM TaskBundle tb WHERE tb.isDeleted = false AND (tb.name ILIKE CONCAT('%', :keyword, '%') OR (tb.description IS NOT NULL AND tb.description ILIKE CONCAT('%', :keyword, '%')))")
    List<TaskBundle> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * Find task bundles with pagination and filtering including schedule type.
     * Uses native SQL with explicit CAST to avoid PostgreSQL null-type inference errors.
     * status and scheduleType are passed as strings (enum .name()) from the service layer.
     */
    @Query(value = "SELECT tb.* FROM task_bundles tb " +
           "LEFT JOIN bundle_schedules s ON tb.id = s.task_bundle_id " +
           "WHERE tb.is_deleted = false " +
           "AND (CAST(:roleId AS BIGINT) IS NULL OR tb.role_id = CAST(:roleId AS BIGINT)) " +
           "AND (CAST(:status AS TEXT) IS NULL OR tb.status = CAST(:status AS TEXT)) " +
           "AND (CAST(:scheduleType AS TEXT) IS NULL OR s.schedule_type = CAST(:scheduleType AS TEXT)) " +
           "AND (CAST(:keyword AS TEXT) IS NULL OR tb.name ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') " +
           "OR (tb.description IS NOT NULL AND tb.description ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%'))) " +
           "ORDER BY tb.created_at DESC",
           nativeQuery = true)
    List<TaskBundle> findWithFilters(@Param("roleId") Long roleId,
                                    @Param("status") String status,
                                    @Param("scheduleType") String scheduleType,
                                    @Param("keyword") String keyword);

    /**
     * Count bundles by status
     */
    long countByStatus(BundleStatus status);
}
