package com.lab.atlasmentor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;

/**
 * One week slot within a create/update template request. {@code weekNumber} is the week's
 * identity (1-4) - it is matched against the template's existing weeks by number, not by a
 * separate week id, since a template's 4 week slots always exist and are never individually
 * added/removed. {@code dayRangeStart}/{@code dayRangeEnd} are deliberately not accepted here -
 * they're always computed server-side from weekNumber + the template's cycleMonth.
 */
@Data
public class WeeklyAccountabilityWeekRequest {
    @NotNull(message = "weekNumber is required")
    private Integer weekNumber;

    /** Display/reminder text only - see WeeklyAccountabilityResponseService. Optional. */
    private DayOfWeek dueWeekday;

    @Valid
    private List<WeeklyAccountabilityQuestionRequest> questions;
}
