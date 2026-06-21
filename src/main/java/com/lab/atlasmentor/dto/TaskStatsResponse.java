package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatsResponse {
    private long openTasks;
    private String openTasksTrend;
    private long inProgress;
    private String inProgressTrend;
    private long overdue;
    private String overdueTrend;
    private long completedThisWeek;
    private String completedThisWeekTrend;
}
