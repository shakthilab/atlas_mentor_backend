package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnerDashboardResponse {

    private Summary summary;
    private EarningsOverview earningsOverview;
    private PayoutStatusSection payoutStatus;
    private List<RecentStudent> recentStudents;
    private QuickStats quickStats;

    // ==================== SECTION 1: KPI Cards ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {
        private KpiCard studentsReferred;
        private KpiCard assignedAmount;
        private KpiCard paidAmount;
        private KpiCard pendingBalance;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KpiCard {
        private String value;
        private String prefix;
        private String suffix;
        private String trend;
        private String trendColor;
    }

    // ==================== SECTION 2: Earnings Overview Line Chart ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EarningsOverview {
        private List<String> labels;
        private EarningsDatasets datasets;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EarningsDatasets {
        private List<Double> assigned;
        private List<Double> paid;
    }

    // ==================== SECTION 3: Payout Status Donut Chart ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayoutStatusSection {
        private long totalPayouts;
        private PayoutDistribution distribution;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayoutDistribution {
        private long assigned;
        private long paid;
        private long pending;
        private long partial;
        private long dispute;
    }

    // ==================== SECTION 4: Recent Students Table ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentStudent {
        private String name;
        private String enrolled;
        private String status;
        private String amount;
        private int progress;
    }

    // ==================== SECTION 5: Quick Stats Sidebar ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuickStats {
        private long activeResources;
        private long openDisputes;
        private String partnerSince;
        private String assignedManager;
    }
}
