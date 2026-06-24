package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskIdOrderByAssignedAtDesc(Long taskId);

    List<TaskAssignment> findByUserId(Long userId);

    Optional<TaskAssignment> findByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN FETCH ta.user WHERE ta.task.id = :taskId")
    List<TaskAssignment> findAssigneesWithDetails(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(ta) FROM TaskAssignment ta WHERE ta.task.id = :taskId")
    Long countAssigneesByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.user.id = :userId")
    void deleteByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM TaskAssignment ta WHERE ta.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT ta.task.id FROM TaskAssignment ta WHERE ta.user.id = :userId")
    List<Long> findTaskIdsByUserId(@Param("userId") Long userId);
}
