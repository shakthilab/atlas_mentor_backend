package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.BundleExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BundleExecution entity operations.
 * Provides execution history tracking and audit functionality.
 */
@Repository
public interface BundleExecutionRepository extends JpaRepository<BundleExecution, Long> {
    
    /**
     * Find executions by task bundle
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId ORDER BY be.executionDate DESC")
    List<BundleExecution> findByTaskBundleIdOrderByExecutionDateDesc(@Param("taskBundleId") Long taskBundleId);

    /**
     * Find latest execution by task bundle
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId ORDER BY be.executionDate DESC LIMIT 1")
    Optional<BundleExecution> findFirstByTaskBundleIdOrderByExecutionDateDesc(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find executions by status
     */
    List<BundleExecution> findByExecutionStatusOrderByExecutionDateDesc(String executionStatus);
    
    /**
     * Find executions by date range
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.executionDate BETWEEN :startDate AND :endDate ORDER BY be.executionDate DESC")
    List<BundleExecution> findByExecutionDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find executions by task bundle and date range
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId AND be.executionDate BETWEEN :startDate AND :endDate ORDER BY be.executionDate DESC")
    List<BundleExecution> findByTaskBundleIdAndExecutionDateBetween(@Param("taskBundleId") Long taskBundleId,
                                                                   @Param("startDate") LocalDateTime startDate,
                                                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find executions by execution date
     */
    @Query("SELECT be FROM BundleExecution be WHERE DATE(be.executionDate) = DATE(:date) ORDER BY be.executionDate DESC")
    List<BundleExecution> findByExecutionDate(@Param("date") LocalDateTime date);
    
    /**
     * Find failed executions
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.executionStatus = 'FAILED' ORDER BY be.executionDate DESC")
    List<BundleExecution> findFailedExecutions();
    
    /**
     * Find partial executions
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.executionStatus = 'PARTIAL' ORDER BY be.executionDate DESC")
    List<BundleExecution> findPartialExecutions();
    
    /**
     * Count executions by task bundle
     */
    @Query("SELECT COUNT(be) FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId")
    Long countByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Count successful executions by task bundle
     */
    @Query("SELECT COUNT(be) FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId AND be.executionStatus = 'SUCCESS'")
    Long countSuccessfulByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Count failed executions by task bundle
     */
    @Query("SELECT COUNT(be) FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId AND be.executionStatus = 'FAILED'")
    Long countFailedByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find executions with high task generation
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.tasksGenerated > :threshold ORDER BY be.tasksGenerated DESC")
    List<BundleExecution> findHighVolumeExecutions(@Param("threshold") Integer threshold);
    
    /**
     * Find executions with long duration
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.executionDurationMs > :thresholdMs ORDER BY be.executionDurationMs DESC")
    List<BundleExecution> findSlowExecutions(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find recent executions
     */
    @Query("SELECT be FROM BundleExecution be WHERE be.executionDate >= :since ORDER BY be.executionDate DESC")
    List<BundleExecution> findRecentExecutions(@Param("since") LocalDateTime since);
    
    /**
     * Check if execution exists for bundle on specific date
     */
    @Query("SELECT COUNT(be) > 0 FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId AND DATE(be.executionDate) = DATE(:date)")
    boolean existsByTaskBundleIdAndExecutionDate(@Param("taskBundleId") Long taskBundleId, @Param("date") LocalDateTime date);
    
    /**
     * Get execution statistics
     */
    @Query("SELECT be.executionStatus, COUNT(be), SUM(be.tasksGenerated), AVG(be.executionDurationMs) FROM BundleExecution be WHERE be.taskBundle.id = :taskBundleId GROUP BY be.executionStatus")
    List<Object[]> getExecutionStatsByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find executions by creator
     */
    List<BundleExecution> findByCreatedByOrderByExecutionDateDesc(Long createdBy);
}
