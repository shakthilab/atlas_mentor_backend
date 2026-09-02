package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.enums.TaskStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Admin's Daily Workspace main panel for one employee/day (Part B / Screenshot 2):
 * the 3 completion stats, current Day Approval Workflow stage, and the task table.
 */
@Data
public class DayDetailResponse {
    private Long dayWorkspaceId;
    private Integer dayNumber;
    private LocalDate date;
    /** Display label for {@link #date} - e.g. "Aug 10". */
    private String dateLabel;
    private boolean isWeeklyCheckpointDay;

    private Long employeeId;
    private String employeeName;
    private String branchName;

    /** From this day's own tasks. */
    private Integer dailyCompletionPct;
    /** Aggregate across the ISO week (Mon-Sun) containing this day. */
    private Integer weeklyCompletionPct;
    /** Aggregate across the calendar month containing this day. */
    private Integer monthlyCompletionPct;

    /** Current Day Approval Workflow stage - one of EMPLOYEE / COMPLETED / PARTNER_REVIEW / MANAGER_REVIEW / ADMIN_VERIFIED. */
    private String approvalStage;

    /** Friendly label for approvalStage - "Not Submitted" / "Submitted" / "Branch Partner Review" / "Manager Review" / "Admin Reviewed - Complete". See DayApprovalService#stageLabel. */
    private String currentStep;
    /** 0-4 position of currentStep in the pipeline (0 = not submitted, 4 = fully verified). */
    private Integer currentStepNumber;
    /** Total milestones after submission (Branch Partner / Manager / Admin review) - always 4, for stepper UIs. */
    private Integer totalSteps;

    /**
     * Role that owns the next approve/send-back action - "BRANCH_PARTNER" / "MANAGER" / "ADMIN" -
     * or null if nothing is currently pending (not yet submitted, or already fully verified).
     * See DayApprovalService#nextActionRole.
     */
    private String nextActionRole;
    /**
     * Whether the caller of this request (from their auth token) is allowed to act right now -
     * their role matches {@link #nextActionRole}, or they're ADMIN and something is pending.
     * The backend still enforces this independently on the actual approve/send-back call
     * (DayApprovalService#actOnDay) - this is only so the UI can disable the button up front
     * instead of letting someone click it and get a 403.
     */
    private boolean canCurrentUserAct;

    private List<DayTaskSummaryResponse> tasks;

    @Data
    public static class DayTaskSummaryResponse {
        private Long id;
        private String displayId;
        private String title;
        private Priority priority;
        private TaskStatus status;
        /** Whether at least one proof-section attachment is required before this task can be marked DONE (V35). */
        private Boolean proofRequired;
        private String latestCommentPreview;
        /** This task's own Day Approval Workflow position - see DayApprovalService#applyStepLabels. */
        private String currentStep;
        private String nextStep;

        // ---- Overdue rollover (V23) ----

        /**
         * True when this task doesn't actually belong to the requested day's own
         * day_workspace - it's still OVERDUE/not-DONE from an earlier day and is only being
         * carried forward into this list until it's completed (see
         * EmployeeTreeService#getDayDetail). Its due_date/day_workspace_id are untouched -
         * false for every task that's genuinely this day's own.
         */
        private boolean carriedOver;
        /** This task's real, never-changing due date - distinct from the requested day when carriedOver is true. */
        private LocalDate dueDate;
        /** work_date of the day_workspace this task actually belongs to (differs from the requested day only when carriedOver). */
        private LocalDate originalWorkDate;
        /** Day number (within its own month) of the day_workspace this task actually belongs to. */
        private Integer originalDayNumber;
        /** When this task was actually marked DONE - independent of dueDate. Null until then. See Task#completedAt. */
        private java.time.LocalDateTime completedAt;
    }
}
