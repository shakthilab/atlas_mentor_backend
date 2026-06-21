package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStudentsResponse {
    private List<StatusBreakdown> statusBreakdown;
    private List<TopCountry> topCountries;
    private List<FunnelItem> acquisitionFunnel;
    private List<PieItem> referralCompany;
    private List<String> intakeLabels;
    private List<ChartSeries> intakeCountrySeries;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class StatusBreakdown {
        private String status;
        private long count;
        private int percentage;
        private String color;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TopCountry {
        private int rank;
        private String country;
        private long students;
        private long converted;
        private String conversionRate;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class FunnelItem {
        private String stage;
        private long count;
        private String color;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PieItem {
        private String name;
        private long value;
        private ItemStyle itemStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChartSeries {
        private String name;
        private String type;
        private String stack;
        private String barWidth;
        private List<Long> data;
        private ItemStyle itemStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ItemStyle {
        private String color;
    }
}
