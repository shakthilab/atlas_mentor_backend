package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TimeTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeTrackingRepository extends JpaRepository<TimeTracking, Long> {

    List<TimeTracking> findByTaskIdOrderByStartTimeDesc(Long taskId);

    List<TimeTracking> findByUserIdOrderByStartTimeDesc(Long userId);

    List<TimeTracking> findByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT tt FROM TimeTracking tt WHERE tt.user.id = :userId AND tt.endTime IS NULL ORDER BY tt.startTime DESC")
    Optional<TimeTracking> findActiveTrackingByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(tt.durationInSeconds), 0) FROM TimeTracking tt WHERE tt.task.id = :taskId")
    Long sumDurationByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(tt.durationInSeconds), 0) FROM TimeTracking tt WHERE tt.user.id = :userId AND tt.startTime BETWEEN :start AND :end")
    Long sumDurationByUserIdAndDateRange(@Param("userId") Long userId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT tt FROM TimeTracking tt JOIN FETCH tt.user WHERE tt.task.id = :taskId ORDER BY tt.startTime DESC")
    List<TimeTracking> findByTaskIdWithUser(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(tt) FROM TimeTracking tt WHERE tt.user.id = :userId AND tt.endTime IS NULL")
    Long countActiveSessionsByUserId(@Param("userId") Long userId);
}
