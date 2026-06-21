package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTasksResponse {
    private List<OverdueTask> overdueTasks;
    private List<HeatmapCell> priorityHeatmap;
    private List<String> throughputLabels;
    private List<ChartSeries> throughputSeries;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class OverdueTask {
        private Long taskId;
        private String title;
        private String assignee;
        private String dueDate;
        private String priority;
        private long daysLate;
        private String priorityColor;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class HeatmapCell {
        private String status;
        private String priority;
        private long count;
        private String color;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChartSeries {
        private String name;
        private String type;
        private Boolean smooth;
        private Boolean showSymbol;
        private Integer symbolSize;
        private List<Long> data;
        private ItemStyle itemStyle;
        private AreaStyle areaStyle;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ItemStyle {
        private String color;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AreaStyle {
        private String color;
    }
}
