package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeeklyAccountabilityDuplicateWeekRequest {
    @NotNull(message = "sourceWeekNumber is required")
    private Integer sourceWeekNumber;
}
