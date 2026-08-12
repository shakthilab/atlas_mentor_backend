package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One task the employee has already fixed and resubmitted, awaiting re-review by the
 * stage that originally flagged it (send-back / rework spec, Part 5's "review item").
 * Resolve via POST /api/days/{dayWorkspaceId}/approve with this taskId.
 */
@Data
public class ResubmittedTaskResponse {
    private Long taskId;
    private String displayId;
    private String title;
    private Priority priority;
    private TaskStatus status;

    private Long dayWorkspaceId;
    private LocalDate workDate;

    private Long employeeId;
    private String employeeName;

    private String originalComment;
    private String flaggedByName;
    private LocalDateTime flaggedAt;
    private LocalDateTime resubmittedAt;
}
