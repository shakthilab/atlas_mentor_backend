package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.BundleSchedule;
import com.lab.atlasmentor.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BundleSchedule entity operations.
 * Provides scheduling and execution date management functionality.
 */
@Repository
public interface BundleScheduleRepository extends JpaRepository<BundleSchedule, Long> {
    
    /**
     * Find active schedules by task bundle
     */
    Optional<BundleSchedule> findByTaskBundleIdAndIsActiveTrue(Long taskBundleId);
    
    /**
     * Find all schedules by task bundle
     */
    Optional<BundleSchedule> findByTaskBundleId(Long taskBundleId);
    
    /**
     * Find schedules that should execute today
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.isActive = true AND bs.taskBundle.status = 'ACTIVE'")
    List<BundleSchedule> findActiveSchedules();
    
    /**
     * Find schedules by schedule type
     */
    List<BundleSchedule> findByScheduleTypeAndIsActiveTrue(ScheduleType scheduleType);
    
    /**
     * Find schedules that need next execution date update
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.isActive = true AND bs.taskBundle.status = 'ACTIVE' AND (bs.nextExecutionDate IS NULL OR bs.nextExecutionDate <= :currentDate)")
    List<BundleSchedule> findNeedingNextExecutionUpdate(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find schedules by execution time range
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.isActive = true AND bs.executionTime BETWEEN :startTime AND :endTime")
    List<BundleSchedule> findByExecutionTimeBetween(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
    
    /**
     * Find weekly schedules by execution day
     */
    List<BundleSchedule> findByScheduleTypeAndExecutionDayAndIsActiveTrue(ScheduleType scheduleType, java.time.DayOfWeek executionDay);
    
    /**
     * Find monthly schedules by execution day of month
     */
    List<BundleSchedule> findByScheduleTypeAndExecutionDayOfMonthAndIsActiveTrue(ScheduleType scheduleType, Integer executionDayOfMonth);
    
    /**
     * Find one-time schedules that should execute on specific date
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.scheduleType = 'ONE_TIME' AND bs.isActive = true AND bs.oneTimeExecutionDate IS NOT NULL AND DATE(bs.oneTimeExecutionDate) = :date")
    List<BundleSchedule> findOneTimeSchedulesForDate(@Param("date") LocalDate date);
    
    /**
     * Count active schedules by type
     */
    @Query("SELECT COUNT(bs) FROM BundleSchedule bs WHERE bs.scheduleType = :scheduleType AND bs.isActive = true AND bs.taskBundle.status = 'ACTIVE'")
    Long countActiveByScheduleType(@Param("scheduleType") ScheduleType scheduleType);
    
    /**
     * Find schedules with date range
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.isActive = true AND " +
           "(:startDate IS NULL OR bs.startDate IS NULL OR bs.startDate <= :currentDate) AND " +
           "(:endDate IS NULL OR bs.endDate IS NULL OR bs.endDate >= :currentDate)")
    List<BundleSchedule> findActiveInDateRange(@Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate, 
                                             @Param("currentDate") LocalDate currentDate);
    
    /**
     * Find schedules that have executed recently
     */
    @Query("SELECT bs FROM BundleSchedule bs WHERE bs.lastExecutionDate >= :fromDate AND bs.isActive = true")
    List<BundleSchedule> findRecentlyExecuted(@Param("fromDate") LocalDate fromDate);
    
    /**
     * Check if schedule exists for task bundle
     */
    boolean existsByTaskBundleId(Long taskBundleId);
}
