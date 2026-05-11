package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ScheduleType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for bundle schedule response.
 * Contains scheduling configuration and execution information.
 */
@Data
public class BundleScheduleResponse {
    
    private Long id;
    private ScheduleType scheduleType;
    private LocalTime executionTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private String executionDay; // Day of week name for WEEKLY
    private Integer executionDayOfMonth; // Day number for MONTHLY
    private LocalDateTime oneTimeExecutionDate;
    private Boolean isActive;
    private LocalDate lastExecutionDate;
    private LocalDate nextExecutionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    /**
     * Get formatted execution day for WEEKLY schedules
     */
    public String getFormattedExecutionDay() {
        if (executionDay != null) {
            return executionDay.substring(0, 1).toUpperCase() + executionDay.substring(1).toLowerCase();
        }
        return null;
    }
    
    /**
     * Get formatted schedule type
     */
    public String getFormattedScheduleType() {
        if (scheduleType != null) {
            return scheduleType.name().charAt(0) + scheduleType.name().substring(1).toLowerCase();
        }
        return null;
    }
}
