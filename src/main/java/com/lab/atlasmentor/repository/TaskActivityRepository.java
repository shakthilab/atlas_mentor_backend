package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskActivity;
import com.lab.atlasmentor.enums.TaskAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    @Query("SELECT ta FROM TaskActivity ta WHERE ta.task.id = :taskId ORDER BY ta.createdAt DESC")
    List<TaskActivity> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT ta FROM TaskActivity ta WHERE ta.doneBy.id = :userId ORDER BY ta.createdAt DESC")
    List<TaskActivity> findByDoneByIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ta FROM TaskActivity ta WHERE ta.task.id = :taskId AND ta.doneBy.id = :userId ORDER BY ta.createdAt DESC")
    List<TaskActivity> findByTaskIdAndDoneByIdOrderByCreatedAtDesc(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Query("SELECT ta FROM TaskActivity ta WHERE ta.action = :action ORDER BY ta.createdAt DESC")
    List<TaskActivity> findByActionOrderByCreatedAtDesc(@Param("action") TaskAction action);
}
