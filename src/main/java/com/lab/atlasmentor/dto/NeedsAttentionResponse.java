package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One REFLECT task for an employee, regardless of its date (Part 2 of the send-back /
 * rework spec - GET /api/employees/{employeeId}/needs-attention). Never filtered by
 * "today": a task flagged on a prior day must stay visible and fixable until resubmitted.
 */
@Data
public class NeedsAttentionResponse {
    private Long taskId;
    private String displayId;
    private String title;
    private Priority priority;

    private Long dayWorkspaceId;
    private LocalDate workDate;
    private Integer dayNumber;

    /** Which stage sent it back: PARTNER_REVIEW / MANAGER_REVIEW / ADMIN_VERIFIED. */
    private String flaggedStage;
    private String comment;
    private String flaggedByName;
    private LocalDateTime flaggedAt;
}
