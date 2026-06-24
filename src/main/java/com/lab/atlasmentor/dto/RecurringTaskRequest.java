package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.lab.atlasmentor.enums.RecurringFrequency;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecurringTaskRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Frequency is required")
    private RecurringFrequency frequency;

    @Min(value = 1, message = "Interval must be at least 1")
    private Integer intervalValue = 1;

    private LocalDateTime nextExecutionTime;
}
