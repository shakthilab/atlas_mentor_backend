package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.DayDetailResponse;
import com.lab.atlasmentor.dto.NeedsAttentionResponse;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.exception.UnauthorizedAccessException;
import com.lab.atlasmentor.model.DayWorkspace;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.repository.DayWorkspaceRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Employee-side Day Approval Workflow actions (Part D3). The three approval stages
 * themselves (Partner/Manager/Admin) live in {@link DayApprovalService}; this class only
 * owns the employee's own "I'm done, submit for review" step.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DayWorkspaceService {

    /** day_workspaces.approval_stage values - see V7/V8 migrations. Employee is actively working. */
    public static final String STAGE_EMPLOYEE = "EMPLOYEE";
    /** Submitted by the employee; awaiting the Branch Partner's review. */
    public static final String STAGE_COMPLETED = "COMPLETED";

    private final DayWorkspaceRepository dayWorkspaceRepository;
    private final TaskRepository taskRepository;
    private final EmployeeTreeService employeeTreeService;

    /**
     * Employee submits their day once tasks are in a completed-enough state, moving the
     * Day Approval Workflow from EMPLOYEE to COMPLETED (the Branch Partner's review queue -
     * see DayApprovalService#getPendingApprovals).
     *
     * Submittability rules:
     * <ol>
     *   <li>Every task on the day must be out of TODO, i.e. actually started.
     *       DONE/COMPLETED/IN_PROGRESS/OVERDUE/REFLECT are all fine to submit with - OVERDUE
     *       and REFLECT are deliberately allowed through rather than blocking submission,
     *       since a reviewer may want to see the day as-is rather than have the employee
     *       stuck unable to submit at all.</li>
     *   <li>At least one task must actually be DONE/COMPLETED - a day where every task is
     *       merely IN_PROGRESS satisfies rule 1 but shouldn't be submittable on its own; the
     *       employee needs to have finished something. Skipped for a day with zero tasks
     *       (an intentional rest day - see TemplateInstantiationService's dayHasNoTasks),
     *       since there's nothing to complete.</li>
     * </ol>
     */
    public DayDetailResponse submitDay(Long employeeId, LocalDate date) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        if (!currentUserId.equals(employeeId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new UnauthorizedAccessException("You can only submit your own day");
        }

        DayWorkspace workspace = dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                .orElseThrow(() -> new BusinessException("No day workspace found for employee " + employeeId + " on " + date));

        if (!STAGE_EMPLOYEE.equals(workspace.getApprovalStage())) {
            throw new BusinessException("This day has already been submitted (current stage: " + workspace.getApprovalStage() + ")");
        }

        List<Task> tasks = taskRepository.findByDayWorkspaceId(workspace.getId());
        List<Task> notStarted = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .toList();
        if (!notStarted.isEmpty()) {
            throw new BusinessException("Cannot submit day: " + notStarted.size()
                    + " task(s) are still To Do. Every task must be started before submitting.");
        }

        boolean hasCompletedTask = tasks.stream()
                .anyMatch(t -> t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.COMPLETED
                        || t.getStatus() == TaskStatus.VERIFIED);
        if (!tasks.isEmpty() && !hasCompletedTask) {
            throw new BusinessException("Cannot submit day: at least one task must be completed (Done) before submitting.");
        }

        workspace.setApprovalStage(STAGE_COMPLETED);
        workspace.setUpdatedBy(currentUserId);
        dayWorkspaceRepository.save(workspace);

        // current_step/next_step (V18) mirrors approval_stage for every task not mid its own
        // reflect cycle - none can be yet, since send-back requires a day already submitted.
        for (Task task : tasks) {
            DayApprovalService.applyStepLabels(task);
        }
        taskRepository.saveAll(tasks);

        log.info("Employee {} submitted day {} ({}) - now awaiting Branch Partner review", employeeId, workspace.getId(), date);

        return employeeTreeService.getDayDetail(employeeId, date);
    }

    /**
     * Send-back / rework spec, Part 2: every task currently in REFLECT for this employee,
     * across ALL dates - deliberately not scoped to "today". A task flagged on Aug 9 must
     * stay visible and fixable on Aug 10, Aug 11, etc. until the employee resubmits it
     * (POST /api/tasks/{taskId}/resubmit); the Daily Workspace UI is expected to show this
     * alongside "today's tasks", not instead of them.
     */
    @Transactional(readOnly = true)
    public List<NeedsAttentionResponse> getNeedsAttention(Long employeeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        if (!currentUserId.equals(employeeId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new UnauthorizedAccessException("You can only view your own needs-attention list");
        }

        return taskRepository.findNeedsAttentionByAssignee(employeeId).stream()
                .map(this::toNeedsAttentionResponse)
                .collect(Collectors.toList());
    }

    private NeedsAttentionResponse toNeedsAttentionResponse(Task task) {
        NeedsAttentionResponse response = new NeedsAttentionResponse();
        response.setTaskId(task.getId());
        response.setDisplayId(task.getDisplayId());
        response.setTitle(task.getTitle());
        response.setPriority(task.getPriority());
        DayWorkspace workspace = task.getDayWorkspace();
        if (workspace != null) {
            response.setDayWorkspaceId(workspace.getId());
            response.setWorkDate(workspace.getWorkDate());
            response.setDayNumber(workspace.getDayNumberInMonth());
        }
        response.setFlaggedStage(task.getReflectStage());
        response.setComment(task.getReflectComment());
        response.setFlaggedByName(task.getReflectFlaggedBy() != null ? task.getReflectFlaggedBy().getFullName() : null);
        response.setFlaggedAt(task.getReflectFlaggedAt());
        return response;
    }
}
