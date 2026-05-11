package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for creating bundle schedule configuration.
 * Contains all scheduling parameters for task generation.
 */
@Data
public class CreateBundleScheduleRequest {
    
    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;
    
    @NotNull(message = "Execution time is required")
    private LocalTime executionTime;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private DayOfWeek executionDay; // For WEEKLY schedules
    
    private Integer executionDayOfMonth; // For MONTHLY schedules (1-31)
    
    private LocalDateTime oneTimeExecutionDate; // For ONE_TIME schedules
}
