package com.lab.atlasmentor.security;

import com.lab.atlasmentor.exception.UnauthorizedAccessException;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Doesn't depend on any particular dev-DB state (the local DB only has one populated branch
 * right now, so a real cross-branch curl can't be exercised there) - this exercises
 * AccessScopeService's rules directly against constructed callers/targets instead, which is
 * what the ticket's "partner in one branch attempting to access another branch's data" check
 * is really about: the branch-comparison logic itself, not any specific DB row.
 */
@ExtendWith(MockitoExtension.class)
class AccessScopeServiceTest {

    private static final long BRANCH_CHENNAI = 2L;
    private static final long BRANCH_OTHER = 99L;

    @Mock
    private UserRepository userRepository;

    private AccessScopeService accessScopeService;

    @BeforeEach
    void setUp() {
        accessScopeService = new AccessScopeService(userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId, String role, Long branchId) {
        CustomUserDetails principal = new CustomUserDetails(userId, "user" + userId + "@test.com", role, branchId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private User userInBranch(Long branchId) {
        User user = new User();
        Branch branch = new Branch();
        branch.setId(branchId);
        user.setBranch(branch);
        return user;
    }

    // ==================== ADMIN / ADMINISTRATIVE_ASSISTANT / MANAGER: unrestricted ====================

    @Test
    void adminSeesAnyEmployeeRegardlessOfBranch() {
        loginAs(3L, "ADMIN", null);
        assertDoesNotThrow(() -> accessScopeService.requireEmployeeVisible(999L));
        assertDoesNotThrow(() -> accessScopeService.requireTaskVisible(BRANCH_OTHER, 999L));
    }

    @Test
    void managerIsOrganizationWideNotBranchScoped() {
        // Manager's own branch is Chennai, but the ticket is explicit: Manager is org-wide,
        // so a task/employee in a completely different branch must still be visible.
        loginAs(7L, "MANAGER", BRANCH_CHENNAI);
        assertDoesNotThrow(() -> accessScopeService.requireTaskVisible(BRANCH_OTHER, 999L));
    }

    @Test
    void administrativeAssistantIsAlsoUnrestricted() {
        loginAs(11L, "ADMINISTRATIVE_ASSISTANT", BRANCH_CHENNAI);
        assertDoesNotThrow(() -> accessScopeService.requireTaskVisible(BRANCH_OTHER, 999L));
    }

    // ==================== BRANCH_PARTNER: own branch only ====================

    @Test
    void branchPartnerSeesEmployeeInOwnBranch() {
        loginAs(18L, "BRANCH_PARTNER", BRANCH_CHENNAI);
        when(userRepository.findById(10L)).thenReturn(Optional.of(userInBranch(BRANCH_CHENNAI)));

        assertDoesNotThrow(() -> accessScopeService.requireEmployeeVisible(10L));
    }

    @Test
    void branchPartnerDeniedEmployeeInDifferentBranch() {
        loginAs(18L, "BRANCH_PARTNER", BRANCH_CHENNAI);
        when(userRepository.findById(999L)).thenReturn(Optional.of(userInBranch(BRANCH_OTHER)));

        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class,
                () -> accessScopeService.requireEmployeeVisible(999L));
        assertTrue(ex.getMessage().toLowerCase().contains("branch"));
    }

    @Test
    void branchPartnerDeniedTaskInDifferentBranch() {
        loginAs(18L, "BRANCH_PARTNER", BRANCH_CHENNAI);
        assertThrows(UnauthorizedAccessException.class,
                () -> accessScopeService.requireTaskVisible(BRANCH_OTHER, 999L));
    }

    @Test
    void branchPartnerAllowedTaskInOwnBranch() {
        loginAs(18L, "BRANCH_PARTNER", BRANCH_CHENNAI);
        assertDoesNotThrow(() -> accessScopeService.requireTaskVisible(BRANCH_CHENNAI, 999L));
    }

    // ==================== Individual contributor: self only ====================

    @Test
    void individualContributorSeesOwnData() {
        loginAs(10L, "JUNIOR_COUNSELLOR", BRANCH_CHENNAI);
        assertDoesNotThrow(() -> accessScopeService.requireEmployeeVisible(10L));
        assertDoesNotThrow(() -> accessScopeService.requireTaskVisible(BRANCH_CHENNAI, 10L));
    }

    @Test
    void individualContributorDeniedAnotherEmployeesData() {
        loginAs(10L, "JUNIOR_COUNSELLOR", BRANCH_CHENNAI);
        assertThrows(UnauthorizedAccessException.class, () -> accessScopeService.requireEmployeeVisible(17L));
    }

    @Test
    void individualContributorDeniedColleaguesTaskEvenInSameBranch() {
        // Same branch as the task, but not their own task - still denied. Individual
        // contributors are self-scoped, not branch-scoped.
        loginAs(10L, "JUNIOR_COUNSELLOR", BRANCH_CHENNAI);
        assertThrows(UnauthorizedAccessException.class,
                () -> accessScopeService.requireTaskVisible(BRANCH_CHENNAI, 17L));
    }

    @Test
    void unknownRoleDefaultsToIndividualContributorNotUnrestricted() {
        // A role AccessScopeService doesn't recognize as unrestricted/branch-partner must fail
        // closed (self-only), never fail open.
        loginAs(50L, "WEB_DEV", null);
        assertThrows(UnauthorizedAccessException.class, () -> accessScopeService.requireEmployeeVisible(51L));
        assertDoesNotThrow(() -> accessScopeService.requireEmployeeVisible(50L));
    }
}
