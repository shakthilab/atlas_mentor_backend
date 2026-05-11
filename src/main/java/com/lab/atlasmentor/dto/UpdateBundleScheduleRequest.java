package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ScheduleType;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for updating bundle schedule configuration.
 * Allows partial updates of schedule parameters.
 */
@Data
public class UpdateBundleScheduleRequest {
    
    private ScheduleType scheduleType;
    
    private LocalTime executionTime;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private DayOfWeek executionDay; // For WEEKLY schedules
    
    private Integer executionDayOfMonth; // For MONTHLY schedules (1-31)
    
    private LocalDateTime oneTimeExecutionDate; // For ONE_TIME schedules
    
    private Boolean isActive;
}
