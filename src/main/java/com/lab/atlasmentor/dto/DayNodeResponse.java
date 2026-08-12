package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * One day node in the Employee Tree (Part A3) - matches the sidebar's "Day N" rows
 * interspersed with distinct "Weekly Accountability" rows (day % 7 == 0).
 */
@Data
public class DayNodeResponse {
    private Long dayWorkspaceId;
    private int dayNumber;
    private LocalDate date;
    /** Display label for {@link #date} - e.g. "Aug 10". */
    private String dateLabel;
    private Integer completionPct;
    private boolean isWeeklyCheckpointDay;
    private String approvalStage;
}
