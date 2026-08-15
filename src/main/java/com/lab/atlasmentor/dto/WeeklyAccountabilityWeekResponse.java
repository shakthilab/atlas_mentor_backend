package com.lab.atlasmentor.dto;

import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;

@Data
public class WeeklyAccountabilityWeekResponse {
    private Long id;
    private Integer weekNumber;
    private Integer dayRangeStart;
    private Integer dayRangeEnd;
    private DayOfWeek dueWeekday;
    /** Derived from questions.isEmpty() - never independently settable. */
    private boolean scheduled;
    private List<WeeklyAccountabilityQuestionResponse> questions;
}
