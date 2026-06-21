package com.lab.atlasmentor.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTeamResponse {
    private List<WorkloadItem> workload;
    private List<PieItem> roleBreakdown;
    private List<LeaderboardEntry> leaderboard;
    private List<ApprovalItem> pendingApprovals;
    private List<BranchPerformance> branchPerformance;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class WorkloadItem {
        private Long userId;
        private String name;
        private long totalTasks;
        private long overdueTasks;
        private String barColor;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PieItem {
        private String name;
        private long value;
        private ItemStyle itemStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LeaderboardEntry {
        private int rank;
        private Long userId;
        private String name;
        private String branch;
        private String revenue;
        private double revenueRaw;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ApprovalItem {
        private String label;
        private long count;
        private String badgeClass;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BranchPerformance {
        private Long branchId;
        private String branch;
        private long totalStudents;
        private long activeStudents;
        private String revenue;
        private double revenueRaw;
        private long tasks;
        private long team;
        private int healthScore;
        private String healthColor;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ItemStyle {
        private String color;
    }
}
