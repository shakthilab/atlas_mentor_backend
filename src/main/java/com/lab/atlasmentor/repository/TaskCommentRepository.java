package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @Query("SELECT tc FROM TaskComment tc WHERE tc.task.id = :taskId ORDER BY tc.createdAt DESC")
    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT tc FROM TaskComment tc WHERE tc.commentedBy.id = :userId ORDER BY tc.createdAt DESC")
    List<TaskComment> findByCommentedByIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT tc FROM TaskComment tc WHERE tc.task.id = :taskId AND tc.commentedBy.id = :userId ORDER BY tc.createdAt DESC")
    List<TaskComment> findByTaskIdAndCommentedByIdOrderByCreatedAtDesc(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Query("SELECT tc FROM TaskComment tc WHERE tc.task.id = :taskId")
    List<TaskComment> findByTaskId(@Param("taskId") Long taskId);
}
