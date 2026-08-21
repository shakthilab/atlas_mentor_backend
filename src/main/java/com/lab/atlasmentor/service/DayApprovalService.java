package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.DayApprovalActionRequest;
import com.lab.atlasmentor.dto.DayApprovalResponse;
import com.lab.atlasmentor.dto.PendingApprovalResponse;
import com.lab.atlasmentor.dto.ResubmittedTaskResponse;
import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.exception.UnauthorizedAccessException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Part E - the Day Approval Workflow: Branch Partner (optional) → Branch Manager (mandatory) →
 * Admin (mandatory), enforced by day_workspaces.approval_stage (see V7/V8 migrations, which
 * already define the pipeline this class implements):
 *
 * <pre>
 * EMPLOYEE --(Submit Day, DayWorkspaceService)--&gt; COMPLETED
 *   COMPLETED        --(Branch Partner APPROVE, optional)--&gt; PARTNER_REVIEW
 *   COMPLETED        --(Branch Manager APPROVE, Partner skipped)--&gt; MANAGER_REVIEW
 *   PARTNER_REVIEW    --(Branch Manager APPROVE)--&gt; MANAGER_REVIEW
 *   MANAGER_REVIEW     --(Admin APPROVE)--&gt;         ADMIN_VERIFIED   (final / "Verified")
 * </pre>
 *
 * Branch Partner review is optional: they may approve a day sitting at COMPLETED (advancing it
 * to PARTNER_REVIEW), or do nothing at all. Either way Branch Manager approval is mandatory -
 * Manager is authorized to act on a day at COMPLETED (Partner skipped) or PARTNER_REVIEW
 * (Partner already acted); both land the day at MANAGER_REVIEW (see {@link #isAuthorizedEntryRole}
 * for the entry-stage ownership this widens). Whether Branch Partner actually reviewed a given
 * day is always recoverable after the fact from day_approvals - simply check whether a
 * PARTNER_REVIEW-stage entry exists in that day's trail (see {@link #getApprovalHistory}); no
 * separate flag is needed. Admin remains mandatory and final, unchanged; ADMIN may additionally
 * act at any non-terminal stage as an override, consistent with the ADMIN-override pattern
 * already used in TaskService.
 *
 * <p><b>Send-back / rework (V12)</b> is a deliberately separate concern from the day-level
 * pipeline above. A day already sitting at ADMIN_VERIFIED from yesterday must still be
 * reachable today when a reviewer spots a problem on one task - so SEND_BACK:
 * <ul>
 *   <li>is <b>not</b> gated by the day's current approval_stage - any of the three stages
 *       (Partner/Manager/Admin) can act independently, whenever they're the one reviewing,
 *       not only "whoever's turn is next" in the day's own pipeline;</li>
 *   <li>only ever touches the specific {@code taskIds} listed - never the whole day, and
 *       never {@code day_workspaces.approval_stage};</li>
 *   <li>records which stage flagged each task directly on the {@link Task} row
 *       ({@code reflect_stage}), so resubmission (see {@link TaskService#resubmitTask}) routes
 *       back to that exact stage - tracked per task, not per day, so two tasks on the same day
 *       flagged by two different stages resubmit independently and correctly.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DayApprovalService {

    private static final String STAGE_EMPLOYEE = "EMPLOYEE";
    private static final String STAGE_COMPLETED = "COMPLETED";
    private static final String STAGE_PARTNER_REVIEW = "PARTNER_REVIEW";
    private static final String STAGE_MANAGER_REVIEW = "MANAGER_REVIEW";
    private static final String STAGE_ADMIN_VERIFIED = "ADMIN_VERIFIED";

    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_SEND_BACK = "SEND_BACK";

    private static final String REFLECT_FLAGGED = "FLAGGED";
    private static final String REFLECT_RESUBMITTED = "RESUBMITTED";

    private static final String REVIEW_LABEL_PARTNER = "Branch Partner Review";
    private static final String REVIEW_LABEL_MANAGER = "Manager Review";
    private static final String REVIEW_LABEL_ADMIN = "Admin Review";

    /** The only roles with a formal stage in this fixed Branch Partner → Branch Manager → Admin workflow. */
    private static final List<String> REVIEWER_ROLES = List.of("ADMIN", "MANAGER", "BRANCH_PARTNER");

    private final DayWorkspaceRepository dayWorkspaceRepository;
    private final DayApprovalRepository dayApprovalRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final UserRepository userRepository;

    // ==================== E1: pending review queue (day-level, first-time review) ====================

    @Transactional(readOnly = true)
    public List<PendingApprovalResponse> getPendingApprovals(String requestedStage) {
        var currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getRole();
        String reviewStage = resolveReviewStage(role, requestedStage);
        List<String> targetApprovalStages = approvalStagesAwaitingReviewStage(reviewStage);

        List<DayWorkspace> pending;
        if ("ADMIN".equalsIgnoreCase(role)) {
            pending = dayWorkspaceRepository.findByApprovalStageInOrderByWorkDateAsc(targetApprovalStages);
        } else {
            Long branchId = requireBranch(currentUser);
            pending = dayWorkspaceRepository.findByApprovalStageInAndBranchIdOrderByWorkDateAsc(targetApprovalStages, branchId);
        }

        return pending.stream().map(this::toPendingApprovalResponse).collect(Collectors.toList());
    }

    private PendingApprovalResponse toPendingApprovalResponse(DayWorkspace workspace) {
        PendingApprovalResponse response = new PendingApprovalResponse();
        response.setDayWorkspaceId(workspace.getId());
        User employee = workspace.getEmployee();
        response.setEmployeeId(employee != null ? employee.getId() : null);
        response.setEmployeeName(employee != null ? employee.getFullName() : null);
        Branch branch = workspace.getBranch();
        response.setBranchId(branch != null ? branch.getId() : null);
        response.setBranchName(branch != null ? branch.getName() : null);
        response.setWorkDate(workspace.getWorkDate());
        response.setDayNumber(workspace.getDayNumberInMonth());
        response.setDailyCompletionPct(workspace.getDailyCompletionPct() != null
                ? workspace.getDailyCompletionPct().intValue() : null);
        response.setApprovalStage(workspace.getApprovalStage());
        return response;
    }

    // ==================== Resubmitted-task review queue (per-task re-review, V12) ====================

    /**
     * Tasks the employee has already fixed and resubmitted (Part 5's "review item"),
     * awaiting the specific stage that originally flagged them - independent of the day's
     * own approval_stage, which never moved for these. Resolve via POST .../approve with
     * that taskId (APPROVE closes the cycle, SEND_BACK re-flags it).
     */
    @Transactional(readOnly = true)
    public List<ResubmittedTaskResponse> getResubmittedTasks(String requestedStage) {
        var currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getRole();
        String reviewStage = resolveReviewStage(role, requestedStage);

        List<Task> tasks;
        if ("ADMIN".equalsIgnoreCase(role)) {
            tasks = taskRepository.findResubmittedByStage(reviewStage);
        } else {
            Long branchId = requireBranch(currentUser);
            tasks = taskRepository.findResubmittedByStageAndBranch(reviewStage, branchId);
        }

        return tasks.stream().map(this::toResubmittedTaskResponse).collect(Collectors.toList());
    }

    private ResubmittedTaskResponse toResubmittedTaskResponse(Task task) {
        ResubmittedTaskResponse response = new ResubmittedTaskResponse();
        response.setTaskId(task.getId());
        response.setDisplayId(task.getDisplayId());
        response.setTitle(task.getTitle());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        DayWorkspace workspace = task.getDayWorkspace();
        if (workspace != null) {
            response.setDayWorkspaceId(workspace.getId());
            response.setWorkDate(workspace.getWorkDate());
        }
        User employee = task.getAssignedTo();
        response.setEmployeeId(employee != null ? employee.getId() : null);
        response.setEmployeeName(employee != null ? employee.getFullName() : null);
        response.setOriginalComment(task.getReflectComment());
        response.setFlaggedByName(task.getReflectFlaggedBy() != null ? task.getReflectFlaggedBy().getFullName() : null);
        response.setFlaggedAt(task.getReflectFlaggedAt());
        response.setResubmittedAt(task.getReflectResubmittedAt());
        return response;
    }

    // ==================== E2: approve / send back ====================

    public void actOnDay(Long dayWorkspaceId, DayApprovalActionRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getRole();
        Long currentUserId = currentUser.getUserId();

        DayWorkspace workspace = dayWorkspaceRepository.findById(dayWorkspaceId)
                .orElseThrow(() -> new BusinessException("Day workspace not found: " + dayWorkspaceId));

        if (!"ADMIN".equalsIgnoreCase(role)) {
            Long userBranchId = currentUser.getBranchId();
            Long workspaceBranchId = workspace.getBranch() != null ? workspace.getBranch().getId() : null;
            if (userBranchId == null || !userBranchId.equals(workspaceBranchId)) {
                throw new UnauthorizedAccessException("Cannot review a day outside your own branch");
            }
        }

        User approver = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found: " + currentUserId));

        boolean hasTaskIds = request.getTaskIds() != null && !request.getTaskIds().isEmpty();

        if (ACTION_APPROVE.equals(request.getAction())) {
            if (hasTaskIds) {
                approveResubmittedTasks(workspace, request, approver, role);
            } else {
                approveDayLevel(workspace, request, approver, role);
            }
        } else if (ACTION_SEND_BACK.equals(request.getAction())) {
            sendBackTasks(workspace, request, approver, role);
        } else {
            // Unreachable in practice - DayApprovalActionRequest.action is @Pattern-validated
            // to APPROVE|SEND_BACK before this method is ever called - but fail loudly rather
            // than silently falling through if that ever changes.
            throw new BusinessException("Unknown approval action: " + request.getAction());
        }
    }

    /**
     * The normal day-level pipeline advance (Part E). Stage ownership: Branch Partner review is
     * optional, so COMPLETED accepts either BRANCH_PARTNER or MANAGER (see
     * {@link #isAuthorizedEntryRole}); the resulting approval_stage is always the acting role's
     * own review stage - not necessarily "the stage that owns the current one" - so a Manager
     * acting on COMPLETED (Partner skipped) lands the day on MANAGER_REVIEW directly, same
     * destination as when they act on PARTNER_REVIEW (Partner already approved).
     */
    private void approveDayLevel(DayWorkspace workspace, DayApprovalActionRequest request, User approver, String role) {
        String currentStage = workspace.getApprovalStage();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        // Re-approval of an already-fully-approved day: a no-op stage-wise, but re-runs the
        // verification sweep below. This is the ONLY way a task's status ever moves from
        // DONE to VERIFIED (see verifyEligibleTasks) - so a task that turned DONE, or had its
        // reflect cycle resolved (see approveResubmittedTasks), after the day already reached
        // ADMIN_VERIFIED stays parked at DONE until ADMIN re-approves the day to pick it up.
        // ADMIN-only: non-admins have no standing stage left to act from once the day is final.
        if (isAdmin && STAGE_ADMIN_VERIFIED.equals(currentStage)) {
            workspace.setUpdatedBy(approver.getId());
            dayWorkspaceRepository.save(workspace);
            saveDayApproval(workspace, approver, STAGE_ADMIN_VERIFIED, ACTION_APPROVE, request.getComment());
            verifyEligibleTasks(workspace, approver);
            log.info("Day {} re-approved at ADMIN_VERIFIED by user {} - re-swept tasks for late completions",
                    workspace.getId(), approver.getId());
            return;
        }

        String reviewStage = reviewStageOwningApprovalStage(currentStage);
        if (reviewStage == null) {
            throw new BusinessException("This day is not currently awaiting review (stage: " + currentStage + ")");
        }
        if (!isAdmin && !isAuthorizedEntryRole(currentStage, role)) {
            String requiredRole = requiredRoleFor(reviewStage);
            throw new UnauthorizedAccessException(role + " cannot act at the " + reviewStage + " stage (owned by " + requiredRole + ")");
        }
        // ADMIN overriding takes the same destination the normal owner of this currentStage
        // would have produced; a non-ADMIN acting always lands on their own review stage
        // (which is what makes Manager-from-COMPLETED and Manager-from-PARTNER_REVIEW both
        // resolve to MANAGER_REVIEW).
        String actingStageLabel = isAdmin ? reviewStage : requiredRoleReviewStage(role);

        workspace.setApprovalStage(actingStageLabel);
        workspace.setUpdatedBy(approver.getId());
        dayWorkspaceRepository.save(workspace);
        refreshStepLabelsForDay(workspace);

        saveDayApproval(workspace, approver, actingStageLabel, ACTION_APPROVE, request.getComment());
        log.info("Day {} approved at {} stage by user {} ({}) - advanced to {}",
                workspace.getId(), actingStageLabel, approver.getId(), role, actingStageLabel);

        // Final stage: the whole review chain (Partner (if it happened) -> Manager -> Admin)
        // has now signed off on this day. Every task that's DONE/COMPLETED and not sitting mid
        // its own separate re-review cycle (reflectState != null - see sendBackTasks/
        // resubmitTask, V12) graduates to VERIFIED, the terminal "no more changes" state (see
        // TaskStatus#VERIFIED). A task still awaiting re-review is deliberately left at DONE -
        // Admin's day-level approval is not a substitute for the flagging reviewer actually
        // re-checking the fix. It's picked up once that reviewer resolves the reflect cycle
        // (approveResubmittedTasks) and ADMIN re-approves the day (see approveDayLevel).
        if (STAGE_ADMIN_VERIFIED.equals(actingStageLabel)) {
            verifyEligibleTasks(workspace, approver);
        }
    }

    /**
     * DONE/COMPLETED -> VERIFIED is a one-way door, and this is its only door: no other code
     * path in the app may set TaskStatus.VERIFIED. Skips anything still mid its own reflect
     * cycle (reflectState != null) - that task needs its flagging reviewer to resolve the
     * cycle first (approveResubmittedTasks), then ADMIN to re-approve the day to pick it up
     * here. Runs once when the day first reaches ADMIN_VERIFIED, and again any time ADMIN
     * re-approves an already-verified day (see approveDayLevel) - the latter is what catches a
     * task whose reflect cycle just closed, or simply wasn't DONE yet, on some earlier pass.
     */
    private void verifyEligibleTasks(DayWorkspace workspace, User approver) {
        List<Task> dayTasks = taskRepository.findByDayWorkspaceId(workspace.getId());
        for (Task task : dayTasks) {
            boolean isDone = task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.COMPLETED;
            if (!isDone || task.getReflectState() != null) {
                continue;
            }
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.VERIFIED);
            task.setUpdatedBy(approver.getId());
            taskRepository.save(task);

            TaskActivity activity = new TaskActivity(task, TaskAction.STATUS_CHANGED,
                    oldStatus.name(), TaskStatus.VERIFIED.name(), approver,
                    "Verified - day fully approved through Admin");
            activity.setCreatedBy(approver.getId());
            taskActivityRepository.save(activity);
        }
    }

    /**
     * Closes the reflect cycle on specific RESUBMITTED tasks (Part 5's "review item", resolved
     * by whichever stage originally flagged it). Only ever clears the reflect fields - never
     * touches {@code task.status}. DONE -> VERIFIED has exactly one door, {@link
     * #verifyEligibleTasks} (run from {@link #approveDayLevel} when ADMIN approves, or
     * re-approves, the day) - so a task resolved here on a day that's already ADMIN_VERIFIED
     * stays at DONE until ADMIN re-approves the day to sweep it up. (Previously this method
     * also had a separate "late completion" branch that set VERIFIED directly for a task that
     * simply wasn't DONE yet when the day's sweep ran; that's now redundant with - and would
     * have violated - the single-door rule, since re-approving the day is a strictly simpler
     * way to catch the exact same task up.)
     */
    private void approveResubmittedTasks(DayWorkspace workspace, DayApprovalActionRequest request, User approver, String role) {
        List<Task> dayTasks = taskRepository.findByDayWorkspaceId(workspace.getId());
        for (Long taskId : request.getTaskIds()) {
            Task task = dayTasks.stream().filter(t -> t.getId().equals(taskId)).findFirst()
                    .orElseThrow(() -> new BusinessException("Task " + taskId + " does not belong to this day"));

            if (!REFLECT_RESUBMITTED.equals(task.getReflectState())) {
                throw new BusinessException("Task " + taskId + " is not currently awaiting re-review (reflect state: "
                        + task.getReflectState() + ")");
            }
            if (!"ADMIN".equalsIgnoreCase(role) && !requiredRoleReviewStage(role).equals(task.getReflectStage())) {
                throw new UnauthorizedAccessException(role + " cannot re-review a task flagged by " + task.getReflectStage());
            }

            task.setReflectState(null);
            task.setReflectStage(null);
            task.setReflectComment(null);
            task.setReflectFlaggedAt(null);
            task.setReflectFlaggedBy(null);
            task.setReflectResubmittedAt(null);
            task.setUpdatedBy(approver.getId());

            // Reflect cycle is closed - fall back to wherever the day's own pipeline currently
            // stands (status itself is untouched; see javadoc above).
            applyStepLabels(task);
            taskRepository.save(task);

            boolean dayFullyApproved = STAGE_ADMIN_VERIFIED.equals(workspace.getApprovalStage());
            boolean isDone = task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.COMPLETED;

            // No dedicated TaskAction for "re-review resolved" - reusing STATUS_CHANGED with
            // null old/new (TaskService#formatActivityMessage renders this as a plain
            // comment-driven line, not a misleading "changed status from null to null").
            // Deliberately not introducing a brand-new TaskAction constant here: TaskStatus
            // turned out to be guarded by a DB-level CHECK constraint not mirrored in this
            // migrations folder (see V11) - a same-shaped surprise on task_activity.action
            // is exactly the kind of risk not worth taking for one cosmetic activity line.
            String note = "resolved the re-review"
                    + (isDone && dayFullyApproved ? " - awaiting Admin re-approval of the day to verify" : "")
                    + (request.getComment() != null && !request.getComment().isBlank()
                    ? " — " + request.getComment() : "");
            TaskActivity activity = new TaskActivity(task, TaskAction.STATUS_CHANGED, null, null, approver, note);
            activity.setCreatedBy(approver.getId());
            taskActivityRepository.save(activity);
        }
        log.info("Day {} - {} resubmitted task(s) re-review resolved by {} ({})",
                workspace.getId(), request.getTaskIds().size(), approver.getId(), role);
    }

    /**
     * Sends specific task(s) back to REFLECT. Deliberately independent of the day's own
     * approval_stage (Part 1) and never mutates it (Part 3) - only the listed tasks change.
     */
    private void sendBackTasks(DayWorkspace workspace, DayApprovalActionRequest request, User approver, String role) {
        if (STAGE_EMPLOYEE.equals(workspace.getApprovalStage())) {
            throw new BusinessException("Cannot send back a day that has not been submitted yet");
        }
        List<Long> taskIds = request.getTaskIds();
        if (taskIds == null || taskIds.isEmpty()) {
            // Explicit decision, not a silent default: sending back a day now only ever
            // reopens the task(s) named - there is no "send back the whole day" mode any
            // more (see the class javadoc and the report back to the team on this choice).
            throw new BusinessException(
                    "SEND_BACK requires at least one taskId - specify which task(s) need rework, not the whole day");
        }

        String actingStage = "ADMIN".equalsIgnoreCase(role) ? STAGE_ADMIN_VERIFIED : requiredRoleReviewStage(role);
        if (actingStage == null) {
            throw new UnauthorizedAccessException(role + " is not a review stage in this workflow");
        }

        List<Task> dayTasks = taskRepository.findByDayWorkspaceId(workspace.getId());
        for (Long taskId : taskIds) {
            Task task = dayTasks.stream().filter(t -> t.getId().equals(taskId)).findFirst()
                    .orElseThrow(() -> new BusinessException("Task " + taskId + " does not belong to this day"));

            TaskStatus oldStatus = task.getStatus();
            task.setReflectPreviousStatus(oldStatus.name());
            // completed_at (V23): flagged back to REFLECT means it's no longer actually done -
            // resubmitTask restamps this once the employee fixes it and it's DONE again.
            if (oldStatus == TaskStatus.DONE || oldStatus == TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
            }
            task.setStatus(TaskStatus.REFLECT);
            task.setReflectState(REFLECT_FLAGGED);
            task.setReflectStage(actingStage);
            task.setReflectComment(request.getComment());
            task.setReflectFlaggedAt(LocalDateTime.now());
            task.setReflectFlaggedBy(approver);
            task.setReflectResubmittedAt(null);
            task.setUpdatedBy(approver.getId());
            applyStepLabels(task);
            taskRepository.save(task);

            TaskActivity activity = new TaskActivity(task, TaskAction.STATUS_CHANGED,
                    oldStatus.name(), TaskStatus.REFLECT.name(), approver, request.getComment());
            activity.setCreatedBy(approver.getId());
            taskActivityRepository.save(activity);

            // Also surfaced as a real comment on the task (not just the activity line), so
            // it shows up where an employee naturally looks - the Comments tab (Part C2).
            if (request.getComment() != null && !request.getComment().isBlank()) {
                TaskComment comment = new TaskComment(task, request.getComment(), approver);
                comment.setCreatedBy(approver.getId());
                comment.setUpdatedBy(approver.getId());
                taskCommentRepository.save(comment);

                TaskActivity commentActivity = new TaskActivity(task, TaskAction.COMMENT_ADDED, null, null, approver);
                commentActivity.setCreatedBy(approver.getId());
                taskActivityRepository.save(commentActivity);
            }
        }

        // Day-level audit trail entry either way (Part 3) - day_workspaces.approval_stage is
        // untouched; day_approvals is purely the "who sent what back, when" record here.
        saveDayApproval(workspace, approver, actingStage, ACTION_SEND_BACK, request.getComment());
        log.info("Day {} - {} task(s) {} sent back by {} ({}) at {} stage",
                workspace.getId(), taskIds.size(), taskIds, approver.getId(), role, actingStage);
    }

    private void saveDayApproval(DayWorkspace workspace, User approver, String stage, String action, String comment) {
        DayApproval approval = new DayApproval();
        approval.setDayWorkspace(workspace);
        approval.setApprover(approver);
        approval.setStage(stage);
        approval.setAction(action);
        approval.setComment(comment);
        approval.setActedAt(LocalDateTime.now());
        approval.setCreatedBy(approver.getId());
        approval.setUpdatedBy(approver.getId());
        dayApprovalRepository.save(approval);
    }

    // ==================== E3: approval history ====================

    @Transactional(readOnly = true)
    public List<DayApprovalResponse> getApprovalHistory(Long dayWorkspaceId) {
        DayWorkspace workspace = dayWorkspaceRepository.findById(dayWorkspaceId)
                .orElseThrow(() -> new BusinessException("Day workspace not found: " + dayWorkspaceId));

        var currentUser = SecurityUtils.getCurrentUser();
        boolean isReviewer = REVIEWER_ROLES.contains(currentUser.getRole() == null ? "" : currentUser.getRole().toUpperCase());
        boolean isOwner = workspace.getEmployee() != null && currentUser.getUserId().equals(workspace.getEmployee().getId());
        if (!isReviewer && !isOwner) {
            throw new UnauthorizedAccessException("Cannot view approval history for another employee's day");
        }

        return dayApprovalRepository.findByDayWorkspaceIdOrderByActedAtAsc(dayWorkspaceId).stream()
                .map(this::toDayApprovalResponse)
                .collect(Collectors.toList());
    }

    private DayApprovalResponse toDayApprovalResponse(DayApproval approval) {
        DayApprovalResponse response = new DayApprovalResponse();
        response.setId(approval.getId());
        response.setStage(approval.getStage());
        response.setAction(approval.getAction());
        response.setComment(approval.getComment());
        response.setApproverId(approval.getApprover() != null ? approval.getApprover().getId() : null);
        response.setApproverName(approval.getApprover() != null ? approval.getApprover().getFullName() : null);
        response.setActedAt(approval.getActedAt());
        return response;
    }

    /**
     * Friendly label for a day's current approval_stage - one step in the pipeline documented
     * on this class (EMPLOYEE -> COMPLETED -> PARTNER_REVIEW -> MANAGER_REVIEW ->
     * ADMIN_VERIFIED). Used by EmployeeTreeService's admin day-detail panel to show progress
     * as "Submitted" / "Branch Partner Review" / "Manager Review" / "Admin Reviewed - Complete"
     * instead of the raw DB constant.
     */
    public static String stageLabel(String approvalStage) {
        if (approvalStage == null) {
            return "Not Submitted";
        }
        return switch (approvalStage) {
            case STAGE_EMPLOYEE -> "Not Submitted";
            case STAGE_COMPLETED -> "Submitted";
            case STAGE_PARTNER_REVIEW -> "Branch Partner Review";
            case STAGE_MANAGER_REVIEW -> "Manager Review";
            case STAGE_ADMIN_VERIFIED -> "Admin Reviewed - Complete";
            default -> approvalStage;
        };
    }

    /** 1-based position of {@link #stageLabel} in the pipeline (0 = not yet submitted, 4 = fully verified). */
    public static int stageNumber(String approvalStage) {
        if (approvalStage == null) {
            return 0;
        }
        return switch (approvalStage) {
            case STAGE_EMPLOYEE -> 0;
            case STAGE_COMPLETED -> 1;
            case STAGE_PARTNER_REVIEW -> 2;
            case STAGE_MANAGER_REVIEW -> 3;
            case STAGE_ADMIN_VERIFIED -> 4;
            default -> 0;
        };
    }

    // ==================== Part-task current_step / next_step (V18) ====================

    /**
     * Recomputes and stamps a task's {@code current_step}/{@code next_step} (V18) from its
     * own reflect cycle if it has one open, otherwise from its day workspace's
     * approval_stage. Callers still need to {@code taskRepository.save(task)} themselves -
     * this only mutates the in-memory entity, same convention as the rest of this class.
     *
     * <p>Call this any time either input could have changed: task creation, the day's
     * approval_stage advancing (submit / partner-manager-admin approve), a task being sent
     * back, resubmitted, or its reflect cycle being resolved.
     */
    public static void applyStepLabels(Task task) {
        String reflectState = task.getReflectState();
        String reflectStage = task.getReflectStage();

        if (REFLECT_FLAGGED.equals(reflectState)) {
            // Employee's turn: fix it, then it goes to whichever stage flagged it.
            task.setCurrentStep("Reflect");
            task.setNextStep(reviewLabel(reflectStage));
            return;
        }
        if (REFLECT_RESUBMITTED.equals(reflectState)) {
            // Reviewer's turn: outcome (approved / sent back again) isn't known yet, so
            // there's no single "next" to name.
            task.setCurrentStep("Awaiting " + reviewLabel(reflectStage));
            task.setNextStep(null);
            return;
        }

        DayWorkspace workspace = task.getDayWorkspace();
        if (workspace == null) {
            // Not part of the Day Approval Workflow at all (e.g. a manual/legacy task never
            // attached to a day) - the concept doesn't apply.
            task.setCurrentStep(null);
            task.setNextStep(null);
            return;
        }

        String approvalStage = workspace.getApprovalStage();
        switch (approvalStage == null ? STAGE_EMPLOYEE : approvalStage) {
            case STAGE_EMPLOYEE -> {
                task.setCurrentStep("Not Submitted");
                task.setNextStep("Submitted");
            }
            case STAGE_COMPLETED -> {
                task.setCurrentStep("Submitted");
                task.setNextStep(REVIEW_LABEL_PARTNER);
            }
            case STAGE_PARTNER_REVIEW -> {
                task.setCurrentStep(REVIEW_LABEL_PARTNER);
                task.setNextStep(REVIEW_LABEL_MANAGER);
            }
            case STAGE_MANAGER_REVIEW -> {
                task.setCurrentStep(REVIEW_LABEL_MANAGER);
                task.setNextStep(REVIEW_LABEL_ADMIN);
            }
            case STAGE_ADMIN_VERIFIED -> {
                task.setCurrentStep("Verified");
                task.setNextStep(null);
            }
            default -> {
                task.setCurrentStep("Not Submitted");
                task.setNextStep("Submitted");
            }
        }
    }

    /** The reviewer-facing label for a review-stage constant, used as a "next step" / "awaiting" value. */
    private static String reviewLabel(String stage) {
        if (stage == null) {
            return "Review";
        }
        return switch (stage) {
            case STAGE_PARTNER_REVIEW -> REVIEW_LABEL_PARTNER;
            case STAGE_MANAGER_REVIEW -> REVIEW_LABEL_MANAGER;
            case STAGE_ADMIN_VERIFIED -> REVIEW_LABEL_ADMIN;
            default -> stage;
        };
    }

    /**
     * Bulk-refreshes current_step/next_step for every task on a day whose own approval_stage
     * just moved, skipping any task mid its own independent reflect cycle (V12) - that task's
     * step stays Reflect/Awaiting-review-based until that cycle resolves, regardless of where
     * the day's pipeline itself is.
     */
    private void refreshStepLabelsForDay(DayWorkspace workspace) {
        List<Task> dayTasks = taskRepository.findByDayWorkspaceId(workspace.getId());
        for (Task task : dayTasks) {
            if (task.getReflectState() != null) {
                continue;
            }
            applyStepLabels(task);
            taskRepository.save(task);
        }
    }

    // ==================== Stage <-> role mapping ====================
    // day_workspaces.approval_stage pipeline (V7/V8): EMPLOYEE -> COMPLETED -> PARTNER_REVIEW
    // -> MANAGER_REVIEW -> ADMIN_VERIFIED. Each of the three review stages below is named
    // after the role that OWNS it - "PARTNER_REVIEW" means "this is the Branch Partner's
    // review", not "already partner-reviewed".

    private Long requireBranch(CustomUserDetails currentUser) {
        Long branchId = currentUser.getBranchId();
        if (branchId == null) {
            throw new BusinessException("User must be assigned to a branch to view the review queue");
        }
        return branchId;
    }

    /** Which review-stage label a role owns; used both for E1's stage param and E2's ownership check. */
    private String resolveReviewStage(String role, String requestedStage) {
        String ownStage = requiredRoleReviewStage(role);
        if (requestedStage == null || requestedStage.isBlank()) {
            return ownStage != null ? ownStage : STAGE_ADMIN_VERIFIED;
        }
        String normalized = requestedStage.trim().toUpperCase();
        if (!"ADMIN".equalsIgnoreCase(role) && !normalized.equals(ownStage)) {
            throw new UnauthorizedAccessException(role + " may only view the " + ownStage + " queue");
        }
        return normalized;
    }

    /** The review-stage label a role owns, or null if that role owns none of the three. */
    private String requiredRoleReviewStage(String role) {
        return switch (role == null ? "" : role.toUpperCase()) {
            case "BRANCH_PARTNER" -> STAGE_PARTNER_REVIEW;
            case "MANAGER" -> STAGE_MANAGER_REVIEW;
            case "ADMIN" -> STAGE_ADMIN_VERIFIED;
            default -> null;
        };
    }

    /** The role required to act at a given review-stage label. */
    private static String requiredRoleFor(String reviewStage) {
        return switch (reviewStage) {
            case STAGE_PARTNER_REVIEW -> "BRANCH_PARTNER";
            case STAGE_MANAGER_REVIEW -> "MANAGER";
            case STAGE_ADMIN_VERIFIED -> "ADMIN";
            default -> throw new BusinessException("Unknown review stage: " + reviewStage);
        };
    }

    /**
     * Non-ADMIN roles authorized to approve/send-back a day currently at
     * {@code currentApprovalStage} - the stage-ownership check {@link #approveDayLevel} enforces.
     * Branch Partner review is optional (see class javadoc): COMPLETED accepts BOTH
     * BRANCH_PARTNER (the normal path) and MANAGER (Branch Partner skipped). PARTNER_REVIEW
     * still requires MANAGER either way - Manager approval is always mandatory. MANAGER_REVIEW
     * has no non-ADMIN owner (Admin's mandatory, final stage - reached only via the ADMIN
     * override every stage already gets). ADMIN itself is handled by the caller before this is
     * consulted, not listed here.
     */
    private static boolean isAuthorizedEntryRole(String currentApprovalStage, String role) {
        return switch (currentApprovalStage) {
            case STAGE_COMPLETED -> "BRANCH_PARTNER".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
            case STAGE_PARTNER_REVIEW -> "MANAGER".equalsIgnoreCase(role);
            default -> false;
        };
    }

    /**
     * Whether {@code role} (ADMIN always may, as an override) is currently authorized to
     * approve/send-back a day at {@code approvalStage} - mirrors the exact gating
     * {@link #approveDayLevel} enforces, so callers like EmployeeTreeService#getDayDetail can
     * size a "can I act on this" UI flag without duplicating the stage-ownership rules (and now
     * that COMPLETED accepts either BRANCH_PARTNER or MANAGER, a single {@link #nextActionRole}
     * string is no longer enough to answer this on its own).
     */
    public static boolean canRoleAct(String approvalStage, String role) {
        if (approvalStage == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return reviewStageOwningApprovalStage(approvalStage) != null;
        }
        return isAuthorizedEntryRole(approvalStage, role);
    }

    /**
     * Role that owns the next approve/send-back action for a day at this {@code approvalStage},
     * or null if nothing is currently pending (not yet submitted - EMPLOYEE - or already fully
     * verified - ADMIN_VERIFIED). Mirrors the exact gating {@link #approveDayLevel} enforces,
     * so a UI can disable review buttons for anyone who isn't this role (ADMIN always may act
     * too, same override as everywhere else in this class) instead of relying on the 403 to
     * find out. See EmployeeTreeService#getDayDetail, which surfaces this on DayDetailResponse.
     */
    public static String nextActionRole(String approvalStage) {
        String reviewStage = reviewStageOwningApprovalStage(approvalStage);
        return reviewStage == null ? null : requiredRoleFor(reviewStage);
    }

    /**
     * Which day_workspaces.approval_stage value(s) a review-stage's first-time-review queue is
     * filtered on. Branch Partner's queue is unchanged (COMPLETED only - what they may
     * optionally review). Manager's queue now covers BOTH COMPLETED (Branch Partner hasn't
     * acted - Manager may act directly, Partner review being optional) and PARTNER_REVIEW
     * (Branch Partner already approved) - Manager needs visibility into everything they're
     * authorized to act on, mirroring {@link #isAuthorizedEntryRole}. Admin's queue is
     * unchanged (MANAGER_REVIEW only).
     */
    private List<String> approvalStagesAwaitingReviewStage(String reviewStage) {
        return switch (reviewStage) {
            case STAGE_PARTNER_REVIEW -> List.of(STAGE_COMPLETED);
            case STAGE_MANAGER_REVIEW -> List.of(STAGE_COMPLETED, STAGE_PARTNER_REVIEW);
            case STAGE_ADMIN_VERIFIED -> List.of(STAGE_MANAGER_REVIEW);
            default -> throw new BusinessException("Unknown review stage: " + reviewStage);
        };
    }

    /** Inverse of {@link #approvalStagesAwaitingReviewStage}: which review-stage owns a given current approval_stage. */
    private static String reviewStageOwningApprovalStage(String currentApprovalStage) {
        return switch (currentApprovalStage) {
            case STAGE_COMPLETED -> STAGE_PARTNER_REVIEW;
            case STAGE_PARTNER_REVIEW -> STAGE_MANAGER_REVIEW;
            case STAGE_MANAGER_REVIEW -> STAGE_ADMIN_VERIFIED;
            default -> null; // EMPLOYEE (not submitted yet) or ADMIN_VERIFIED (already final)
        };
    }
}
