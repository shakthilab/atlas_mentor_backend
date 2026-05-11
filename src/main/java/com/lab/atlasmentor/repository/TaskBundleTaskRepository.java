package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskBundleTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TaskBundleTask entity operations.
 * Provides task management within bundles functionality.
 */
@Repository
public interface TaskBundleTaskRepository extends JpaRepository<TaskBundleTask, Long> {
    
    /**
     * Find active tasks by task bundle
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isActive = true AND tbt.isDeleted = false ORDER BY tbt.taskOrder")
    List<TaskBundleTask> findActiveByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find all tasks by task bundle
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isDeleted = false ORDER BY tbt.taskOrder")
    List<TaskBundleTask> findByTaskBundleIdAndIsDeletedFalse(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find task bundle task by title and bundle (for uniqueness within bundle)
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.title = :title AND tbt.isDeleted = false")
    Optional<TaskBundleTask> findByTaskBundleIdAndTitleAndIsDeletedFalse(@Param("taskBundleId") Long taskBundleId, @Param("title") String title);
    
    /**
     * Count active tasks in a bundle
     */
    @Query("SELECT COUNT(tbt) FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isActive = true AND tbt.isDeleted = false")
    Long countActiveByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find tasks by multiple task bundles
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id IN :taskBundleIds AND tbt.isActive = true AND tbt.isDeleted = false ORDER BY tbt.taskBundle.id, tbt.taskOrder")
    List<TaskBundleTask> findActiveByTaskBundleIds(@Param("taskBundleIds") List<Long> taskBundleIds);
    
    /**
     * Find tasks by priority
     */
    List<TaskBundleTask> findByPriorityAndIsActiveTrueAndIsDeletedFalse(com.lab.atlasmentor.enums.Priority priority);
    
    /**
     * Search tasks by title or description within a bundle
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isDeleted = false AND " +
           "(LOWER(tbt.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tbt.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<TaskBundleTask> searchByTaskBundleIdAndKeyword(@Param("taskBundleId") Long taskBundleId, @Param("keyword") String keyword);
    
    /**
     * Find tasks with default due days set
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.defaultDueDays IS NOT NULL AND tbt.isActive = true AND tbt.isDeleted = false")
    List<TaskBundleTask> findWithDefaultDueDays(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Get maximum task order in a bundle
     */
    @Query("SELECT COALESCE(MAX(tbt.taskOrder), 0) FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isDeleted = false")
    Integer getMaxTaskOrderByTaskBundleId(@Param("taskBundleId") Long taskBundleId);
    
    /**
     * Find tasks that need reordering (gaps in sequence)
     */
    @Query("SELECT tbt FROM TaskBundleTask tbt WHERE tbt.taskBundle.id = :taskBundleId AND tbt.isActive = true AND tbt.isDeleted = false ORDER BY tbt.taskOrder")
    List<TaskBundleTask> findForReordering(@Param("taskBundleId") Long taskBundleId);
}
