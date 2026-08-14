package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Dynamic filtering query - simplified to avoid parameter type issues
    @Query(value = "SELECT t.* FROM tasks t WHERE t.is_deleted = false AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:assigneeId IS NULL OR t.assigned_to = :assigneeId) AND " +
            "(:branchId IS NULL OR t.branch_id = :branchId) AND " +
            "(:priority IS NULL OR t.priority = :priority) AND " +
            "(:createdBy IS NULL OR t.created_by = :createdBy) AND " +
            "(:keyword IS NULL OR t.title LIKE CONCAT('%', :keyword, '%') OR t.description LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:overdue IS NULL OR (:overdue = true AND t.due_date < CURRENT_DATE AND t.status != 'COMPLETED')) AND " +
            "(:search IS NULL OR t.title LIKE CONCAT('%', :search, '%') OR t.description LIKE CONCAT('%', :search, '%')) " +
            "ORDER BY t.created_at DESC",
            nativeQuery = true)
    List<Task> findTasksWithFilters(
            @Param("status") String status,
            @Param("assigneeId") Long assigneeId,
            @Param("branchId") Long branchId,
            @Param("priority") String priority,
            @Param("createdBy") Long createdBy,
            @Param("keyword") String keyword,
            @Param("overdue") Boolean overdue,
            @Param("search") String search
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

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :userId AND t.status = :status")
    long countByAssignedToIdAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

    // Search functionality
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    List<Task> searchByKeyword(@Param("keyword") String keyword);

    // Soft delete support
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.isDeleted = false")
    Optional<Task> findActiveTaskById(@Param("id") Long id);
    
    // Branch-based access control methods
    @Query(value = "SELECT t.* FROM tasks t WHERE t.is_deleted = false AND " +
            "(:isAdmin = true OR (:isAdmin = false AND (t.assigned_to = :currentUserId OR t.created_by = :currentUserId)))", nativeQuery = true)
    List<Task> findAllWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, @Param("currentUserId") Long currentUserId);
    
    @Query(value = "SELECT t.* FROM tasks t WHERE (:isAdmin = true OR t.branch_id = :branchId) AND t.is_deleted = false AND t.status = :status", nativeQuery = true)
    List<Task> findByStatusWithAccess(@Param("status") String status, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT t.* FROM tasks t WHERE (:isAdmin = true OR t.branch_id = :branchId) AND t.is_deleted = false AND t.assigned_to = :userId", nativeQuery = true)
    List<Task> findByAssignedToIdWithAccess(@Param("userId") Long userId, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT COUNT(t) FROM tasks t WHERE (:isAdmin = true OR t.branch_id = :branchId) AND t.is_deleted = false AND t.status = :status", nativeQuery = true)
    long countByStatusWithAccess(@Param("status") String status, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);

    // Role-based task fetching methods with pagination support
    
    // For ADMIN: return all tasks
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false ORDER BY t.createdAt DESC")
    Page<Task> findAllTasksForAdmin(Pageable pageable);

    // For ADMIN with status filter
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<Task> findAllTasksForAdminWithStatus(@Param("status") TaskStatus status, Pageable pageable);

    // For MANAGER: return tasks where branchId = user.branchId
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId ORDER BY t.createdAt DESC")
    Page<Task> findTasksByBranchForManager(@Param("branchId") Long branchId, Pageable pageable);

    // For MANAGER with status filter
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<Task> findTasksByBranchForManagerWithStatus(@Param("branchId") Long branchId, @Param("status") TaskStatus status, Pageable pageable);
    
    // For SENIOR_COUNSELLOR: return tasks where branchId = user.branchId AND (assignedTo = user.id OR assignedTo IN juniorIds)
    @Query(value = "SELECT t.* FROM tasks t WHERE t.is_deleted = false AND t.branch_id = :branchId AND " +
            "(t.assigned_to = :seniorId OR t.assigned_to IN (:juniorIds)) ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tasks t WHERE t.is_deleted = false AND t.branch_id = :branchId AND " +
            "(t.assigned_to = :seniorId OR t.assigned_to IN (:juniorIds))",
           nativeQuery = true)
    Page<Task> findTasksForSeniorCounsellor(@Param("branchId") Long branchId, @Param("seniorId") Long seniorId, @Param("juniorIds") List<Long> juniorIds, Pageable pageable);

    // For SENIOR_COUNSELLOR with status filter
    @Query(value = "SELECT t.* FROM tasks t WHERE t.is_deleted = false AND t.branch_id = :branchId AND " +
            "(t.assigned_to = :seniorId OR t.assigned_to IN (:juniorIds)) AND " +
            "(:status IS NULL OR t.status = :status) ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tasks t WHERE t.is_deleted = false AND t.branch_id = :branchId AND " +
            "(t.assigned_to = :seniorId OR t.assigned_to IN (:juniorIds)) AND " +
            "(:status IS NULL OR t.status = :status)",
           nativeQuery = true)
    Page<Task> findTasksForSeniorCounsellorWithStatus(@Param("branchId") Long branchId, @Param("seniorId") Long seniorId, @Param("juniorIds") List<Long> juniorIds, @Param("status") String status, Pageable pageable);
    
    // For JUNIOR_COUNSELLOR: return tasks where branchId = user.branchId AND assignedTo = user.id
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND t.assignedTo.id = :juniorId ORDER BY t.createdAt DESC")
    Page<Task> findTasksForJuniorCounsellor(@Param("branchId") Long branchId, @Param("juniorId") Long juniorId, Pageable pageable);

    // For JUNIOR_COUNSELLOR with status filter
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND t.assignedTo.id = :juniorId AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<Task> findTasksForJuniorCounsellorWithStatus(@Param("branchId") Long branchId, @Param("juniorId") Long juniorId, @Param("status") TaskStatus status, Pageable pageable);
    
    // Legacy non-paginated methods for backward compatibility
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false ORDER BY t.createdAt DESC")
    List<Task> findAllTasksForAdminList();

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId ORDER BY t.createdAt DESC")
    List<Task> findTasksByBranchForManagerList(@Param("branchId") Long branchId);

    @Query(value = "SELECT t.* FROM tasks t WHERE t.is_deleted = false AND t.branch_id = :branchId AND " +
            "(t.assigned_to = :seniorId OR t.assigned_to IN (:juniorIds)) ORDER BY t.created_at DESC", nativeQuery = true)
    List<Task> findTasksForSeniorCounsellorList(@Param("branchId") Long branchId, @Param("seniorId") Long seniorId, @Param("juniorIds") List<Long> juniorIds);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND t.assignedTo.id = :juniorId ORDER BY t.createdAt DESC")
    List<Task> findTasksForJuniorCounsellorList(@Param("branchId") Long branchId, @Param("juniorId") Long juniorId);
    
    // Task bundle related queries
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.taskBundle.id = :bundleId AND t.assignedTo.id = :userId AND t.executionDate = :executionDate AND t.title = :title")
    List<Task> findByAssignedToIdAndTaskBundleIdAndExecutionDateAndTitleAndIsDeletedFalse(
            @Param("userId") Long userId, 
            @Param("bundleId") Long bundleId, 
            @Param("executionDate") LocalDate executionDate, 
            @Param("title") String title);
    
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.taskBundle.id = :bundleId")
    List<Task> findByTaskBundleIdAndIsDeletedFalse(@Param("bundleId") Long bundleId);
    
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.taskBundle.id = :bundleId AND t.title = :title")
    Optional<Task> findByTaskBundleIdAndTitleAndIsDeletedFalse(@Param("bundleId") Long bundleId, @Param("title") String title);
    
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.sourceType = :sourceType AND t.executionDate BETWEEN :startDate AND :endDate")
    List<Task> findBySourceTypeAndExecutionDateBetweenAndIsDeletedFalse(
            @Param("sourceType") com.lab.atlasmentor.enums.TaskSource sourceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // REFLECT excluded alongside the terminal statuses: a task sent back for rework is
    // already in a review-managed state (see DayApprovalService#sendBackTasks) - the cron
    // shouldn't overwrite that with OVERDUE just because its original due date has passed.
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate < :currentDate AND t.status NOT IN ('OVERDUE', 'COMPLETED', 'DONE', 'REFLECT', 'VERIFIED')")
    List<Task> findTasksToMarkOverdue(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.dueDate < :currentDate AND t.status NOT IN ('OVERDUE', 'COMPLETED', 'DONE', 'REFLECT', 'VERIFIED')")
    Long countTasksToMarkOverdue(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.dueDate < :currentDate AND t.status != 'OVERDUE'")
    Long countByDueDateBeforeAndStatusNotAndIsDeletedFalse(@Param("currentDate") LocalDate currentDate, @Param("status") TaskStatus status);
    
    @Query("SELECT t.priority, COUNT(t) FROM Task t WHERE t.isDeleted = false AND t.dueDate < :currentDate AND t.status = 'OVERDUE' GROUP BY t.priority")
    List<Object[]> countOverdueTasksByPriority(@Param("currentDate") LocalDate currentDate);
    
    @Query(value = "SELECT DATEDIFF(CURRENT_DATE, t.due_date) as daysOverdue, COUNT(t) as count FROM tasks t WHERE t.is_deleted = false AND t.due_date < CURRENT_DATE AND t.status = 'OVERDUE' GROUP BY DATEDIFF(CURRENT_DATE, t.due_date) ORDER BY daysOverdue", nativeQuery = true)
    List<Object[]> countOverdueTasksByAge(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :userId AND t.dueDate < :currentDate AND t.status != 'OVERDUE'")
    List<Task> findByAssignedToIdAndDueDateBeforeAndStatusNotAndIsDeletedFalse(
            @Param("userId") Long userId, 
            @Param("currentDate") LocalDate currentDate, 
            @Param("status") TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.branch.id = :branchId AND t.dueDate < :currentDate AND t.status != 'OVERDUE'")
    List<Task> findByBranchIdAndDueDateBeforeAndStatusNotAndIsDeletedFalse(
            @Param("branchId") Long branchId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") TaskStatus status);

    @Modifying
    @Query("UPDATE Task t SET t.assignedTo = null WHERE t.assignedTo.id = :userId")
    void nullifyAssignedToByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Task t SET t.assignedBy = null WHERE t.assignedBy.id = :userId")
    void nullifyAssignedByByUserId(@Param("userId") Long userId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT status, priority, COUNT(*) FROM tasks WHERE is_deleted = false AND created_at >= :from GROUP BY status, priority", nativeQuery = true)
    List<Object[]> countGroupByStatusAndPriority(@Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', created_at), 'IYYY-IW') as week_label, COUNT(*) as cnt " +
           "FROM tasks WHERE is_deleted = false AND created_at >= :from " +
           "GROUP BY DATE_TRUNC('week', created_at) ORDER BY DATE_TRUNC('week', created_at)", nativeQuery = true)
    List<Object[]> countWeeklyCreatedTasks(@Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', updated_at), 'IYYY-IW') as week_label, COUNT(*) as cnt " +
           "FROM tasks WHERE is_deleted = false AND status = 'COMPLETED' AND updated_at >= :from " +
           "GROUP BY DATE_TRUNC('week', updated_at) ORDER BY DATE_TRUNC('week', updated_at)", nativeQuery = true)
    List<Object[]> countWeeklyCompletedTasks(@Param("from") LocalDateTime from);

    @Query(value = "SELECT t.id, t.title, u.first_name, u.last_name, t.due_date, t.priority, " +
           "(CURRENT_DATE - t.due_date) as days_late " +
           "FROM tasks t LEFT JOIN users u ON t.assigned_to = u.id " +
           "WHERE t.is_deleted = false AND t.status = 'OVERDUE' " +
           "ORDER BY t.due_date ASC LIMIT 5", nativeQuery = true)
    List<Object[]> findOverdueTasksWithAssignee();

    @Query(value = "SELECT u.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as emp_name, " +
           "COUNT(t.id) as total_tasks, " +
           "SUM(CASE WHEN t.status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_cnt " +
           "FROM users u " +
           "JOIN roles r ON r.id = u.role_id " +
           "LEFT JOIN tasks t ON t.assigned_to = u.id AND t.is_deleted = false AND t.created_at >= :from " +
           "WHERE r.name NOT IN ('ADMIN', 'STUDENT', 'REFERRAL', 'COMPANY') AND u.status = 'ACTIVE' " +
           "GROUP BY u.id, emp_name ORDER BY total_tasks DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findEmployeeTaskLoad(@Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE is_deleted = false AND status IN ('PENDING', 'IN_PROGRESS')", nativeQuery = true)
    Long countActiveTasks();

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE is_deleted = false AND status = 'OVERDUE'", nativeQuery = true)
    Long countOverdueTasks();

    // ==================== BRANCH-SCOPED DASHBOARD QUERIES ====================

    @Query(value = "SELECT status, priority, COUNT(*) FROM tasks WHERE is_deleted = false AND branch_id = :branchId AND created_at >= :from GROUP BY status, priority", nativeQuery = true)
    List<Object[]> countGroupByStatusAndPriorityForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', created_at), 'IYYY-IW') as week_label, COUNT(*) as cnt " +
           "FROM tasks WHERE is_deleted = false AND branch_id = :branchId AND created_at >= :from " +
           "GROUP BY DATE_TRUNC('week', created_at) ORDER BY DATE_TRUNC('week', created_at)", nativeQuery = true)
    List<Object[]> countWeeklyCreatedTasksForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', updated_at), 'IYYY-IW') as week_label, COUNT(*) as cnt " +
           "FROM tasks WHERE is_deleted = false AND branch_id = :branchId AND status = 'COMPLETED' AND updated_at >= :from " +
           "GROUP BY DATE_TRUNC('week', updated_at) ORDER BY DATE_TRUNC('week', updated_at)", nativeQuery = true)
    List<Object[]> countWeeklyCompletedTasksForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT t.id, t.title, u.first_name, u.last_name, t.due_date, t.priority, " +
           "(CURRENT_DATE - t.due_date) as days_late " +
           "FROM tasks t LEFT JOIN users u ON t.assigned_to = u.id " +
           "WHERE t.is_deleted = false AND t.status = 'OVERDUE' AND t.branch_id = :branchId " +
           "ORDER BY t.due_date ASC LIMIT 5", nativeQuery = true)
    List<Object[]> findOverdueTasksWithAssigneeForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT u.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as emp_name, " +
           "COUNT(t.id) as total_tasks, " +
           "SUM(CASE WHEN t.status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_cnt " +
           "FROM users u " +
           "JOIN roles r ON r.id = u.role_id " +
           "LEFT JOIN tasks t ON t.assigned_to = u.id AND t.is_deleted = false AND t.branch_id = :branchId AND t.created_at >= :from " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' AND u.branch_id = :branchId " +
           "GROUP BY u.id, emp_name ORDER BY total_tasks DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findEmployeeTaskLoadForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE is_deleted = false AND branch_id = :branchId AND status IN ('PENDING','IN_PROGRESS')", nativeQuery = true)
    Long countActiveTasksForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE is_deleted = false AND branch_id = :branchId AND status = 'OVERDUE'", nativeQuery = true)
    Long countOverdueTasksForBranch(@Param("branchId") Long branchId);

    // ==================== EMPLOYEE TREE / DAY WORKSPACE QUERIES ====================

    /** All (non-deleted) tasks belonging to one day workspace - the Day Detail task table (Part B). */
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dayWorkspace.id = :dayWorkspaceId ORDER BY t.createdAt ASC")
    List<Task> findByDayWorkspaceId(@Param("dayWorkspaceId") Long dayWorkspaceId);

    /** All (non-deleted) day-workspace-driven tasks for one employee across a date range - used to
     *  compute daily/weekly/monthly completion % (Part B) without relying on the not-reliably-maintained
     *  DayWorkspace.dailyCompletionPct column. */
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dayWorkspace.employee.id = :employeeId " +
           "AND t.dayWorkspace.workDate BETWEEN :start AND :end")
    List<Task> findByDayWorkspaceEmployeeIdAndWorkDateBetween(
            @Param("employeeId") Long employeeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Overall day-workspace task completion per employee (Employee Tree completion % badge,
     * Part A1) - total vs. done count across every day-workspace-driven task they've ever had.
     * DONE/COMPLETED/VERIFIED are all counted as "done": template-generated tasks use DONE
     * while older manual-flow tasks still use COMPLETED (see
     * TaskService#validateStatusTransition), and VERIFIED is DONE that's since been fully
     * approved (see DayApprovalService#verifyEligibleTasks) - still "done" either way.
     */
    @Query("SELECT t.assignedTo.id, COUNT(t), SUM(CASE WHEN t.status IN ('DONE','COMPLETED','VERIFIED') THEN 1 ELSE 0 END) " +
           "FROM Task t WHERE t.isDeleted = false AND t.dayWorkspace IS NOT NULL AND t.assignedTo.id IN :employeeIds " +
           "GROUP BY t.assignedTo.id")
    List<Object[]> countDayWorkspaceTaskCompletionByAssignee(@Param("employeeIds") List<Long> employeeIds);

    // ==================== SEND-BACK / REWORK TRACKING (V12) ====================

    /** Needs Attention (Part 2): every REFLECT task for this employee, across ALL dates - never filtered by "today". */
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assignedTo.id = :employeeId AND t.status = 'REFLECT' " +
           "ORDER BY t.reflectFlaggedAt DESC")
    List<Task> findNeedsAttentionByAssignee(@Param("employeeId") Long employeeId);

    /** Resubmitted tasks awaiting a given stage's re-review, scoped to one branch (Branch Partner/Manager). */
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.reflectState = 'RESUBMITTED' " +
           "AND t.reflectStage = :stage AND t.branch.id = :branchId ORDER BY t.reflectResubmittedAt ASC")
    List<Task> findResubmittedByStageAndBranch(@Param("stage") String stage, @Param("branchId") Long branchId);

    /** Resubmitted tasks awaiting a given stage's re-review, across all branches (Admin). */
    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.reflectState = 'RESUBMITTED' AND t.reflectStage = :stage " +
           "ORDER BY t.reflectResubmittedAt ASC")
    List<Task> findResubmittedByStage(@Param("stage") String stage);
}
