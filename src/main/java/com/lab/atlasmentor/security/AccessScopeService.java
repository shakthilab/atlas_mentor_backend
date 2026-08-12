package com.lab.atlasmentor.security;

import com.lab.atlasmentor.exception.UnauthorizedAccessException;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Single source of truth for "can the currently authenticated caller see this employee's /
 * task's data" - used by the Employee Tree endpoints (EmployeeTreeService), the "my" endpoints
 * (MyWorkspaceController), and task-detail reads (TaskService#getTaskById and friends).
 *
 * <p>Rules (role comes from the JWT via SecurityUtils - never a client-supplied param, since a
 * param can be spoofed and the JWT claim can't):
 * <ul>
 *   <li>ADMIN / ADMINISTRATIVE_ASSISTANT / MANAGER - unrestricted. MANAGER is deliberately
 *       organization-wide here, not branch-scoped - there is no branch-level manager role.</li>
 *   <li>BRANCH_PARTNER - only data belonging to their own branch. The only branch-scoped
 *       reviewer role.</li>
 *   <li>Everything else (SENIOR_COUNSELLOR, JUNIOR_COUNSELLOR, VIDEO_EDITOR, WEB_DEV, ...) -
 *       only their own data, by user ID. They never see another employee's tree/tasks at all.</li>
 * </ul>
 *
 * <p><b>Scope note:</b> this governs the newer read paths (Employee Tree, day-detail, task
 * detail/comments/attachments/activity). It intentionally does NOT change
 * TaskService#validateTaskUpdate or #fetchAllAccessibleTasks (task create/update/list), which
 * predate this class and use a different, already-shipped convention where MANAGER is
 * branch-scoped alongside BRANCH_PARTNER/ADMINISTRATIVE_ASSISTANT. Unifying those is a separate,
 * bigger change - see the accompanying report.
 */
@Service
@RequiredArgsConstructor
public class AccessScopeService {

    private static final Set<String> UNRESTRICTED_ROLES = Set.of("ADMIN", "ADMINISTRATIVE_ASSISTANT", "MANAGER");
    private static final String BRANCH_PARTNER_ROLE = "BRANCH_PARTNER";

    private final UserRepository userRepository;

    public boolean isUnrestricted(String role) {
        return role != null && UNRESTRICTED_ROLES.contains(role.toUpperCase());
    }

    public boolean isBranchPartner(String role) {
        return BRANCH_PARTNER_ROLE.equalsIgnoreCase(role);
    }

    /** True for any role that isn't one of the branch/org-wide reviewer roles above - i.e. an individual contributor. */
    public boolean isIndividualContributor(String role) {
        return !isUnrestricted(role) && !isBranchPartner(role);
    }

    /**
     * Throws (403 via GlobalExceptionHandler) unless the current caller may view the given
     * employee's tree/day data: unrestricted roles always pass; BRANCH_PARTNER only if
     * employeeId is in their own branch; everyone else only if employeeId is their own.
     */
    public void requireEmployeeVisible(Long employeeId) {
        CustomUserDetails caller = SecurityUtils.getCurrentUser();
        String role = caller.getRole();

        if (isUnrestricted(role)) {
            return;
        }

        if (isBranchPartner(role)) {
            Long callerBranchId = caller.getBranchId();
            Long targetBranchId = userRepository.findById(employeeId).map(User::getBranchId).orElse(null);
            if (callerBranchId == null || targetBranchId == null || !callerBranchId.equals(targetBranchId)) {
                throw new UnauthorizedAccessException("You can only view employees within your own branch");
            }
            return;
        }

        if (!caller.getUserId().equals(employeeId)) {
            throw new UnauthorizedAccessException("You can only view your own data");
        }
    }

    /**
     * Same rule as {@link #requireEmployeeVisible}, applied to a task's own branch/assignee
     * instead of looking an employee up by ID - for task detail/comments/attachments/activity,
     * where the task is already loaded and carries both directly.
     */
    public void requireTaskVisible(Long taskBranchId, Long taskAssignedToId) {
        CustomUserDetails caller = SecurityUtils.getCurrentUser();
        String role = caller.getRole();

        if (isUnrestricted(role)) {
            return;
        }

        if (isBranchPartner(role)) {
            Long callerBranchId = caller.getBranchId();
            if (callerBranchId == null || taskBranchId == null || !callerBranchId.equals(taskBranchId)) {
                throw new UnauthorizedAccessException("You can only view tasks within your own branch");
            }
            return;
        }

        if (taskAssignedToId == null || !caller.getUserId().equals(taskAssignedToId)) {
            throw new UnauthorizedAccessException("You can only view your own tasks");
        }
    }
}
