package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardReferralsResponse {
    private List<ReferralMetric> referralMetrics;
    private List<FunnelItem> referralFunnel;
    private List<TopReferrer> topReferrers;
    private List<String> earningsLabels;
    private List<ChartSeries> earningsPartnerSeries;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReferralMetric {
        private String title;
        private String value;
        private String trend;
        private String trendColor;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class FunnelItem {
        private String stage;
        private long count;
        private String color;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TopReferrer {
        private int rank;
        private String name;
        private String partnerType;
        private long students;
        private String commission;
        private double commissionRaw;
        private double sharePercent;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChartSeries {
        private String name;
        private String type;
        private String stack;
        private String barWidth;
        private List<Double> data;
        private ItemStyle itemStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ItemStyle {
        private String color;
    }
}
