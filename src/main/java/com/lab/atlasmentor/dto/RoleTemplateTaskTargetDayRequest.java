package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * One extra target day for a bulk task-clone request (see {@link RoleTemplateTaskRequest#getTargetDays()}).
 * Mirrors the (dayNumber, month, year) scoping used everywhere else on role templates: null
 * month/year means the recurring day for that dayNumber, non-null scopes it to one calendar
 * month - so a single bulk call can clone tasks across months (e.g. Aug day 23 -> Sep days 1-5).
 */
@Data
public class RoleTemplateTaskTargetDayRequest {
    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    private Integer month;
    private Integer year;
}
