package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardFinancialResponse {
    private List<MetricCard> financialMetrics;
    private List<PayingEntity> topPayingEntities;
    private List<DisputeItem> openDisputes;
    private List<PaymentDistLegend> paymentDistLegends;
    private List<String> paymentsLabels;
    private List<ChartSeries> paymentsStatusSeries;
    private List<PieItem> paymentDistribution;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetricCard {
        private String title;
        private String value;
        private String trend;
        private String trendColor;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PayingEntity {
        private String entity;
        private String type;
        private String amount;
        private String status;
        private String lastActivity;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class DisputeItem {
        private Long id;
        private String entityName;
        private String reason;
        private String amount;
        private String disputedAt;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PaymentDistLegend {
        private String method;
        private long count;
        private String totalAmount;
        private int percentage;
        private String color;
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
    public static class PieItem {
        private String name;
        private double value;
        private ItemStyle itemStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ItemStyle {
        private String color;
    }
}
