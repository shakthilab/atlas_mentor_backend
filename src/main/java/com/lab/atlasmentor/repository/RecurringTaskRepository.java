package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.RecurringTask;
import com.lab.atlasmentor.enums.RecurringFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {

    Optional<RecurringTask> findByTaskId(Long taskId);

    boolean existsByTaskId(Long taskId);

    List<RecurringTask> findByFrequency(RecurringFrequency frequency);

    @Query("SELECT rt FROM RecurringTask rt WHERE rt.nextExecutionTime <= :now ORDER BY rt.nextExecutionTime ASC")
    List<RecurringTask> findDueForExecution(@Param("now") LocalDateTime now);

    @Query("SELECT rt FROM RecurringTask rt WHERE rt.nextExecutionTime BETWEEN :start AND :end")
    List<RecurringTask> findByNextExecutionTimeBetween(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("SELECT rt FROM RecurringTask rt JOIN FETCH rt.task WHERE rt.task.id = :taskId")
    Optional<RecurringTask> findByTaskIdWithDetails(@Param("taskId") Long taskId);
}
