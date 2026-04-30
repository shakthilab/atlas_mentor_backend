package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Basic queries excluding deleted tasks
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false")
    List<Task> findAllActiveTasks();

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :userId")
    List<Task> findByAssignedToId(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedBy.id = :userId")
    List<Task> findByAssignedById(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId")
    List<Task> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.status = :status")
    List<Task> findByStatus(@Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.priority = :priority")
    List<Task> findByPriority(@Param("priority") Priority priority);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate <= :date AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("date") LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate = :date")
    List<Task> findByDueDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false ORDER BY t.createdAt DESC")
    List<Task> findAllOrderByCreatedAtDesc();

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByStatusOrderByCreatedAtDesc(@Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId ORDER BY t.createdAt DESC")
    List<Task> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findByAssignedToIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Dynamic filtering query
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:assigneeId IS NULL OR t.assignedTo.id = :assigneeId) AND " +
           "(:branchId IS NULL OR t.branch.id = :branchId) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:createdBy IS NULL OR t.createdBy.id = :createdBy) AND " +
           "(:keyword IS NULL OR t.title LIKE %:keyword% OR t.description LIKE %:keyword%) AND " +
           "(:overdue IS NULL OR (:overdue = true AND t.dueDate < CURRENT_DATE AND t.status != 'DONE')) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findTasksWithFilters(
            @Param("status") TaskStatus status,
            @Param("assigneeId") Long assigneeId,
            @Param("branchId") Long branchId,
            @Param("priority") Priority priority,
            @Param("createdBy") Long createdBy,
            @Param("keyword") String keyword,
            @Param("overdue") Boolean overdue
    );

    // Combined filter queries
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND t.status = :status")
    List<Task> findByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :userId AND t.status = :status")
    List<Task> findByAssignedToIdAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

    // Count queries
    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.status = :status")
    long countByStatus(@Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.priority = :priority")
    long countByPriority(@Param("priority") Priority priority);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false")
    long countAllActiveTasks();

    // Search functionality
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    List<Task> searchByKeyword(@Param("keyword") String keyword);

    // Soft delete support
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.isDeleted = false")
    Optional<Task> findActiveTaskById(@Param("id") Long id);
}
