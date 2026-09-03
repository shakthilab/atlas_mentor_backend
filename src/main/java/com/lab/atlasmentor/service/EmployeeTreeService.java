package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.DayWorkspace;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskComment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.DayWorkspaceRepository;
import com.lab.atlasmentor.repository.TaskCommentRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.security.AccessScopeService;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backs the Employee Tree sidebar (Branch → Role → Employee → Year → Month → Day →
 * Weekly Accountability, Part A) and the admin Daily Workspace main panel (Part B).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmployeeTreeService {

    /** Same convention used across the rest of the app (e.g. TaskRepository dashboard queries). */
    private static final List<String> NON_EMPLOYEE_ROLES = List.of("ADMIN", "STUDENT", "REFERRAL", "COMPANY");
    private static final Set<TaskStatus> DONE_STATUSES = EnumSet.of(TaskStatus.DONE, TaskStatus.COMPLETED, TaskStatus.VERIFIED);

    private final UserRepository userRepository;
    private final DayWorkspaceRepository dayWorkspaceRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final AccessScopeService accessScopeService;

    // ==================== A1: Branch → Role → Employee tree ====================

    /**
     * ADMIN/ADMINISTRATIVE_ASSISTANT/MANAGER see every branch, unfiltered. BRANCH_PARTNER
     * (the only branch-scoped reviewer role) sees only their own branch - filtered here in the
     * service, not just at the controller's @PreAuthorize, so the query result itself never
     * includes another branch's employees. Individual-contributor roles never reach this method
     * at all (EmployeeTreeController's class-level @PreAuthorize 403s them before the call).
     */
    public EmployeeTreeResponse getEmployeeTree() {
        List<User> employees = userRepository.findActiveEmployeesForTree(NON_EMPLOYEE_ROLES);

        String callerRole = SecurityUtils.getCurrentUserRole();
        if (accessScopeService.isBranchPartner(callerRole)) {
            Long callerBranchId = SecurityUtils.getCurrentBranchId();
            employees = employees.stream()
                    .filter(u -> u.getBranch() != null && u.getBranch().getId().equals(callerBranchId))
                    .collect(Collectors.toList());
        }

        Map<Long, Integer> completionByEmployeeId = computeOverallCompletionPct(
                employees.stream().map(User::getId).collect(Collectors.toList()));

        Map<Long, List<User>> byBranch = employees.stream()
                .collect(Collectors.groupingBy(u -> u.getBranch().getId(), LinkedHashMap::new, Collectors.toList()));

        List<EmployeeTreeResponse.BranchNode> branchNodes = new ArrayList<>();
        for (Map.Entry<Long, List<User>> branchEntry : byBranch.entrySet()) {
            List<User> branchEmployees = branchEntry.getValue();

            Map<Long, List<User>> byRole = branchEmployees.stream()
                    .collect(Collectors.groupingBy(u -> u.getRole().getId(), LinkedHashMap::new, Collectors.toList()));

            List<EmployeeTreeResponse.RoleNode> roleNodes = new ArrayList<>();
            for (Map.Entry<Long, List<User>> roleEntry : byRole.entrySet()) {
                List<User> roleEmployees = roleEntry.getValue();
                var role = roleEmployees.get(0).getRole();

                EmployeeTreeResponse.RoleNode roleNode = new EmployeeTreeResponse.RoleNode();
                roleNode.setRoleId(role.getId());
                roleNode.setRoleName(role.getName());
                roleNode.setRoleDisplayName(role.getDisplayName() != null ? role.getDisplayName() : role.getName());
                roleNode.setEmployeeCount(roleEmployees.size());
                roleNode.setEmployees(roleEmployees.stream().map(u -> {
                    EmployeeTreeResponse.EmployeeNode node = new EmployeeTreeResponse.EmployeeNode();
                    node.setEmployeeId(u.getId());
                    node.setFullName(u.getFullName());
                    node.setCompletionPct(completionByEmployeeId.get(u.getId()));
                    return node;
                }).collect(Collectors.toList()));
                roleNodes.add(roleNode);
            }

            var branch = branchEmployees.get(0).getBranch();
            EmployeeTreeResponse.BranchNode branchNode = new EmployeeTreeResponse.BranchNode();
            branchNode.setBranchId(branch.getId());
            branchNode.setBranchName(branch.getName());
            branchNode.setEmployeeCount(branchEmployees.size());
            branchNode.setRoles(roleNodes);
            branchNodes.add(branchNode);
        }

        EmployeeTreeResponse response = new EmployeeTreeResponse();
        response.setBranches(branchNodes);
        return response;
    }

    /** Overall completion % per employee: done vs. total across every day-workspace task they've ever had. */
    private Map<Long, Integer> computeOverallCompletionPct(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : taskRepository.countDayWorkspaceTaskCompletionByAssignee(employeeIds)) {
            Long employeeId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long done = row[2] == null ? 0 : ((Number) row[2]).longValue();
            result.put(employeeId, total == 0 ? null : (int) Math.round((done * 100.0) / total));
        }
        return result;
    }

    // ==================== A2: Years / Months with real data ====================

    public List<YearNodeResponse> getEmployeeYears(Long employeeId) {
        accessScopeService.requireEmployeeVisible(employeeId);
        List<DayWorkspace> workspaces = dayWorkspaceRepository.findByEmployeeIdOrderByWorkDateAsc(employeeId);

        Map<Integer, Long> countByYear = workspaces.stream()
                .collect(Collectors.groupingBy(dw -> dw.getWorkDate().getYear(), LinkedHashMap::new, Collectors.counting()));

        return countByYear.entrySet().stream()
                .map(e -> new YearNodeResponse(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toList());
    }

    public List<MonthNodeResponse> getEmployeeMonths(Long employeeId, int year) {
        accessScopeService.requireEmployeeVisible(employeeId);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<DayWorkspace> workspaces = dayWorkspaceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, start, end);

        Map<Integer, List<DayWorkspace>> byMonth = workspaces.stream()
                .collect(Collectors.groupingBy(dw -> dw.getWorkDate().getMonthValue(), LinkedHashMap::new, Collectors.toList()));

        List<MonthNodeResponse> months = new ArrayList<>();
        for (Map.Entry<Integer, List<DayWorkspace>> entry : byMonth.entrySet()) {
            int month = entry.getKey();
            List<DayWorkspace> monthWorkspaces = entry.getValue();
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            Integer avgPct = computeCompletionPctForRange(employeeId, monthStart, monthEnd);

            String monthName = monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            months.add(new MonthNodeResponse(month, monthName, monthWorkspaces.size(), avgPct));
        }
        return months;
    }

    public List<DayNodeResponse> getEmployeeDays(Long employeeId, int year, int month) {
        accessScopeService.requireEmployeeVisible(employeeId);
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<DayWorkspace> workspaces = dayWorkspaceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, monthStart, monthEnd);

        return workspaces.stream().map(dw -> {
            DayNodeResponse node = new DayNodeResponse();
            node.setDayWorkspaceId(dw.getId());
            node.setDayNumber(dw.getDayNumberInMonth() != null ? dw.getDayNumberInMonth() : dw.getWorkDate().getDayOfMonth());
            node.setDate(dw.getWorkDate());
            node.setDateLabel(dw.getWorkDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + dw.getWorkDate().getDayOfMonth());
            node.setCompletionPct(computeCompletionPctForRange(employeeId, dw.getWorkDate(), dw.getWorkDate()));
            node.setWeeklyCheckpointDay(Boolean.TRUE.equals(dw.getIsWeeklyCheckpointDay()));
            node.setApprovalStage(dw.getApprovalStage());
            return node;
        }).collect(Collectors.toList());
    }

    // ==================== Part B: Admin day detail ====================

    public DayDetailResponse getDayDetail(Long employeeId, LocalDate date) {
        accessScopeService.requireEmployeeVisible(employeeId);
        DayWorkspace workspace = dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                .orElseThrow(() -> new BusinessException("No day workspace found for employee " + employeeId + " on " + date));

        User employee = workspace.getEmployee();
        List<Task> dayTasks = taskRepository.findByDayWorkspaceId(workspace.getId());

        // Overdue Task Rollover (V23): "today's" task list is the UNION of this day's own
        // tasks with every still-open OVERDUE task for this employee, regardless of which
        // earlier day it actually belongs to - so a task keeps showing up day after day until
        // it's completed, without ever touching its due_date/day_workspace_id. Only applied
        // when the requested day IS today: browsing a past day in the Employee Tree should
        // still show exactly that day's own historical tasks, not retroactively pick up
        // carry-forward tasks that only started rolling after the fact.
        List<Task> displayTasks = dayTasks;
        if (date.equals(LocalDate.now())) {
            List<Task> carriedOver = taskRepository.findCarriedOverOverdueByAssignee(employeeId).stream()
                    .filter(t -> t.getDayWorkspace() == null || !t.getDayWorkspace().getId().equals(workspace.getId()))
                    .toList();
            if (!carriedOver.isEmpty()) {
                displayTasks = new ArrayList<>(dayTasks);
                displayTasks.addAll(carriedOver);
            }
        }

        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());

        DayDetailResponse response = new DayDetailResponse();
        response.setDayWorkspaceId(workspace.getId());
        response.setDayNumber(workspace.getDayNumberInMonth());
        response.setDate(date);
        response.setDateLabel(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth());
        response.setWeeklyCheckpointDay(Boolean.TRUE.equals(workspace.getIsWeeklyCheckpointDay()));
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee != null ? employee.getFullName() : null);
        response.setBranchName(workspace.getBranch() != null ? workspace.getBranch().getName() : null);
        response.setApprovalStage(workspace.getApprovalStage());
        response.setCurrentStep(DayApprovalService.stageLabel(workspace.getApprovalStage()));
        response.setCurrentStepNumber(DayApprovalService.stageNumber(workspace.getApprovalStage()));
        response.setTotalSteps(4);
        String nextActionRole = DayApprovalService.nextActionRole(workspace.getApprovalStage());
        response.setNextActionRole(nextActionRole);
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        // Not just "does currentUserRole match nextActionRole" - Branch Partner review is
        // optional, so a day at COMPLETED is actionable by BOTH BRANCH_PARTNER and MANAGER
        // (see DayApprovalService#canRoleAct), which a single nextActionRole string can't
        // express on its own.
        boolean canAct = DayApprovalService.canRoleAct(workspace.getApprovalStage(), currentUserRole);
        response.setCanCurrentUserAct(canAct);
        response.setDailyCompletionPct(computeCompletionPct(dayTasks));
        response.setWeeklyCompletionPct(computeCompletionPctForRange(employeeId, weekStart, weekEnd));
        response.setMonthlyCompletionPct(computeCompletionPctForRange(employeeId, monthStart, monthEnd));
        response.setTasks(displayTasks.stream()
                .map(t -> toDayTaskSummary(t, workspace.getId()))
                .collect(Collectors.toList()));
        return response;
    }

    private DayDetailResponse.DayTaskSummaryResponse toDayTaskSummary(Task task, Long requestedWorkspaceId) {
        DayDetailResponse.DayTaskSummaryResponse summary = new DayDetailResponse.DayTaskSummaryResponse();
        summary.setId(task.getId());
        summary.setDisplayId(task.getDisplayId());
        summary.setTitle(task.getTitle());
        summary.setPriority(task.getPriority());
        summary.setStatus(task.getStatus());
        summary.setProofRequired(Boolean.TRUE.equals(task.getProofRequired()));
        summary.setCurrentStep(task.getCurrentStep());
        summary.setNextStep(task.getNextStep());
        summary.setDueDate(task.getDueDate());
        summary.setCompletedAt(task.getCompletedAt());

        DayWorkspace taskWorkspace = task.getDayWorkspace();
        boolean carriedOver = taskWorkspace == null || !taskWorkspace.getId().equals(requestedWorkspaceId);
        summary.setCarriedOver(carriedOver);
        if (taskWorkspace != null) {
            summary.setOriginalWorkDate(taskWorkspace.getWorkDate());
            summary.setOriginalDayNumber(taskWorkspace.getDayNumberInMonth());
        }

        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId());
        if (!comments.isEmpty()) {
            String preview = comments.get(0).getComment();
            // A voice-only comment (see AddCommentRequest) has no text - fall back to a
            // placeholder instead of NPEing on preview.length().
            if (preview == null || preview.isBlank()) {
                summary.setLatestCommentPreview("🎤 Voice message");
            } else {
                summary.setLatestCommentPreview(preview.length() > 140 ? preview.substring(0, 140) + "…" : preview);
            }
        }
        return summary;
    }

    // ==================== Shared completion math ====================

    /**
     * done/total across every day-workspace task an employee has in [start, end] (inclusive).
     * A range with zero tasks reads as 100% (nothing to complete = fully done), matching the
     * same convention TemplateInstantiationService already uses for zero-task days.
     */
    private Integer computeCompletionPctForRange(Long employeeId, LocalDate start, LocalDate end) {
        List<Task> tasks = taskRepository.findByDayWorkspaceEmployeeIdAndWorkDateBetween(employeeId, start, end);
        return computeCompletionPct(tasks);
    }

    private Integer computeCompletionPct(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 100;
        }
        long done = tasks.stream().filter(t -> DONE_STATUSES.contains(t.getStatus())).count();
        return (int) Math.round((done * 100.0) / tasks.size());
    }
}
