package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

/**
 * Branch → Role → Employee tree (Part A1). Always returns the full tree with role
 * employee-counts already computed; the branch-collapsed vs. drilled-into-branch views
 * shown in the reference screenshots are both just different client-side renderings of
 * this same payload (collapsed = show branches/roles/counts only, expanded = also render
 * the nested employee lists) - no separate "summary" endpoint needed.
 */
@Data
public class EmployeeTreeResponse {
    private List<BranchNode> branches;

    @Data
    public static class BranchNode {
        private Long branchId;
        private String branchName;
        private int employeeCount;
        private List<RoleNode> roles;
    }

    @Data
    public static class RoleNode {
        private Long roleId;
        private String roleName;
        private String roleDisplayName;
        private int employeeCount;
        private List<EmployeeNode> employees;
    }

    @Data
    public static class EmployeeNode {
        private Long employeeId;
        private String fullName;
        /** Overall completion % badge (e.g. "Sandhya Ramesh — 90%") - null if they have no day-workspace tasks yet. */
        private Integer completionPct;
    }
}
