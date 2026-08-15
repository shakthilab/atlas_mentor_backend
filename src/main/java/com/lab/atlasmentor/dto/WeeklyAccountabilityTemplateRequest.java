package com.lab.atlasmentor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WeeklyAccountabilityTemplateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Role ID is required")
    private Long roleId;

    /** "yyyy-MM", e.g. "2026-08" - normalized server-side to the 1st of that month. */
    @NotBlank(message = "cycleMonth is required")
    private String cycleMonth;

    /**
     * Up to 4 entries, keyed by weekNumber. Any of weeks 1-4 omitted from the request is
     * still created as an unscheduled ("Not scheduled") week - see WeeklyAccountabilityTemplateService.
     */
    @Valid
    private List<WeeklyAccountabilityWeekRequest> weeks;
}
