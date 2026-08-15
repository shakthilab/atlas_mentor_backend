package com.lab.atlasmentor.dto;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * The employee-facing "my-template" shape: the ACTIVE template for the caller's role + current
 * cycle month, resolved down to specifically the week today falls into - never all 4 weeks at
 * once, since an employee only ever answers the current week's questions.
 */
@Data
public class WeeklyAccountabilityMyTemplateResponse {
    private Long templateId;
    private String templateName;
    private Long roleId;
    private LocalDate cycleMonth;
    private Integer weekNumber;
    private Integer dayRangeStart;
    private Integer dayRangeEnd;
    private DayOfWeek dueWeekday;
    private List<WeeklyAccountabilityQuestionResponse> questions;
}
