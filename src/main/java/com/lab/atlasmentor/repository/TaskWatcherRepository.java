package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskWatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskWatcherRepository extends JpaRepository<TaskWatcher, Long> {

    List<TaskWatcher> findByTaskId(Long taskId);

    List<TaskWatcher> findByUserId(Long userId);

    Optional<TaskWatcher> findByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT tw FROM TaskWatcher tw JOIN FETCH tw.user WHERE tw.task.id = :taskId")
    List<TaskWatcher> findWatchersWithUserDetails(@Param("taskId") Long taskId);

    @Query("SELECT tw.user.id FROM TaskWatcher tw WHERE tw.task.id = :taskId")
    List<Long> findWatcherUserIdsByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskWatcher tw WHERE tw.task.id = :taskId AND tw.user.id = :userId")
    void deleteByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
