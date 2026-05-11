package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing scheduling configuration for task bundles.
 * Defines when and how frequently tasks should be generated.
 */
@Entity
@Table(name = "bundle_schedules")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BundleSchedule extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_bundle_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"schedule"})
    private TaskBundle taskBundle;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;
    
    @Column(name = "execution_time", nullable = false)
    private LocalTime executionTime;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_day")
    private DayOfWeek executionDay;
    
    @Column(name = "execution_day_of_month")
    private Integer executionDayOfMonth;
    
    @Column(name = "one_time_execution_date")
    private LocalDateTime oneTimeExecutionDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "last_execution_date")
    private LocalDate lastExecutionDate;
    
    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;
    
    public BundleSchedule() {}
    
    public BundleSchedule(TaskBundle taskBundle, ScheduleType scheduleType, LocalTime executionTime) {
        this.taskBundle = taskBundle;
        this.scheduleType = scheduleType;
        this.executionTime = executionTime;
        this.isActive = true;
    }
    
    /**
     * Get task bundle ID for backward compatibility
     */
    public Long getTaskBundleId() {
        return taskBundle != null ? taskBundle.getId() : null;
    }
    
    /**
     * Set task bundle by ID for backward compatibility
     */
    public void setTaskBundleId(Long taskBundleId) {
        if (taskBundleId == null) {
            this.taskBundle = null;
        } else {
            this.taskBundle = new TaskBundle();
            this.taskBundle.setId(taskBundleId);
        }
    }
    
    /**
     * Check if schedule is currently active
     */
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    /**
     * Check if schedule should execute today
     */
    public boolean shouldExecuteToday(LocalDate today) {
        if (!isCurrentlyActive() || today == null) {
            return false;
        }
        
        // Check date range
        if (startDate != null && today.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        
        // Check based on schedule type
        switch (scheduleType) {
            case DAILY:
                return true;
            case WEEKLY:
                return executionDay != null && today.getDayOfWeek().equals(executionDay);
            case MONTHLY:
                return executionDayOfMonth != null && today.getDayOfMonth() == executionDayOfMonth;
            case ONE_TIME:
                return oneTimeExecutionDate != null && 
                       oneTimeExecutionDate.toLocalDate().equals(today);
            default:
                return false;
        }
    }
    
    /**
     * Calculate next execution date
     */
    public LocalDate calculateNextExecutionDate(LocalDate fromDate) {
        if (!isCurrentlyActive()) {
            return null;
        }
        
        LocalDate nextDate = fromDate != null ? fromDate : LocalDate.now();
        
        switch (scheduleType) {
            case DAILY:
                // Check if execution time has passed today
                if (executionTime != null) {
                    LocalTime now = LocalTime.now();
                    if (now.isBefore(executionTime)) {
                        // Execution time hasn't passed today, schedule for today
                        return nextDate;
                    }
                }
                return nextDate.plusDays(1);
            case WEEKLY:
                if (executionDay != null) {
                    LocalDate nextWeek = nextDate;
                    while (nextWeek.getDayOfWeek() != executionDay) {
                        nextWeek = nextWeek.plusDays(1);
                    }
                    return nextWeek;
                }
                break;
            case MONTHLY:
                if (executionDayOfMonth != null) {
                    LocalDate nextMonth = nextDate.plusMonths(1);
                    int maxDay = nextMonth.lengthOfMonth();
                    int targetDay = Math.min(executionDayOfMonth, maxDay);
                    return nextMonth.withDayOfMonth(targetDay);
                }
                break;
            case ONE_TIME:
                if (oneTimeExecutionDate != null && 
                    oneTimeExecutionDate.toLocalDate().isAfter(nextDate)) {
                    return oneTimeExecutionDate.toLocalDate();
                }
                break;
        }
        
        return null;
    }
}
