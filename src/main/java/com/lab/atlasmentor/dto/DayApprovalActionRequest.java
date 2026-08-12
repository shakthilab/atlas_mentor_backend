package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/** Body for POST /api/days/{dayWorkspaceId}/approve (Part E2 / send-back-rework spec Part 3). */
@Data
public class DayApprovalActionRequest {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "APPROVE|SEND_BACK", message = "Action must be APPROVE or SEND_BACK")
    private String action;

    private String comment;

    /**
     * Which task(s) this action applies to.
     * <p>
     * SEND_BACK: required, non-empty - only these tasks flip to REFLECT; every other task
     * on the day is left untouched. There is no "send back the whole day" mode any more -
     * omitting taskIds is rejected rather than silently reopening every task (see
     * DayApprovalService#actOnDay).
     * <p>
     * APPROVE: optional. Omitted (or applied to a day with no resubmitted tasks) means the
     * normal day-level pipeline advance (Part E). Provided against RESUBMITTED tasks, it
     * instead closes out that/those task(s)' reflect cycle (Part 5's "review item") without
     * touching the day's own approval_stage.
     */
    private List<Long> taskIds;
}
