package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferralSummaryResponse {

    private ReferralCounts referralCounts;
    private PayoutStats payoutStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferralCounts {
        private long total;
        private long active;
        private long inactive;
        private Map<String, Long> byType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutStats {
        private long totalRecords;
        private Map<String, Long> statusCounts;
        private BigDecimal totalAssignedAmount;
        private BigDecimal totalPaidAmount;
        private BigDecimal totalPendingBalance;
        private BigDecimal totalDisputedAmount;
    }
}