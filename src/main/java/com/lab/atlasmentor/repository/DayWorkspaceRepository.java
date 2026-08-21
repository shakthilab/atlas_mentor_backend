package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.DayWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DayWorkspaceRepository extends JpaRepository<DayWorkspace, Long> {
    List<DayWorkspace> findByEmployeeId(Long employeeId);
    Optional<DayWorkspace> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    /**
     * Count of workspaces an employee has ever had. Since one row exists per employee
     * per calendar day (uk_day_workspaces_employee_date), this count + 1 is the employee's
     * next "day number" in whatever template is currently driving their assignments.
     */
    long countByEmployeeId(Long employeeId);

    /**
     * Every workspace an employee has ever had, oldest first - the source of truth for
     * which years/months genuinely have data (Employee Tree Part A2/A3: data-driven, not
     * a static Jan-Dec calendar). Small per-employee dataset (at most one row per calendar
     * day since they started), so grouping into years/months happens in Java rather than
     * fighting Postgres date-extraction functions in JPQL.
     */
    List<DayWorkspace> findByEmployeeIdOrderByWorkDateAsc(Long employeeId);

    List<DayWorkspace> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, LocalDate start, LocalDate end);

    /** Days currently awaiting review at a given stage, scoped to one branch (Partner/Manager). */
    List<DayWorkspace> findByApprovalStageAndBranchIdOrderByWorkDateAsc(String approvalStage, Long branchId);

    /** Days currently awaiting review at a given stage, across all branches (Admin). */
    List<DayWorkspace> findByApprovalStageOrderByWorkDateAsc(String approvalStage);

    /**
     * Days currently awaiting review at any of several stages, scoped to one branch - used by
     * Manager's pending-review queue, which now covers both COMPLETED (Branch Partner review
     * is optional - Manager may act directly) and PARTNER_REVIEW (Branch Partner already acted).
     */
    List<DayWorkspace> findByApprovalStageInAndBranchIdOrderByWorkDateAsc(List<String> approvalStages, Long branchId);

    /** Days currently awaiting review at any of several stages, across all branches (Admin). */
    List<DayWorkspace> findByApprovalStageInOrderByWorkDateAsc(List<String> approvalStages);
}
