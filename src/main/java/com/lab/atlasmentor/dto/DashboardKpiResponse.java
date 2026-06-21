package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardKpiResponse {
    private List<SummaryCard> summaryCards;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryCard {
        private String title;
        private String value;
        private String trend;
        private String trendColor;
        private String prefix;
        private String suffix;
        private List<Double> chartData;
    }
}
