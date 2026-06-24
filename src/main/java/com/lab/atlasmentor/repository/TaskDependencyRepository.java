package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskDependency;
import com.lab.atlasmentor.enums.DependencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByTaskId(Long taskId);

    List<TaskDependency> findByDependsOnTaskId(Long dependsOnTaskId);

    List<TaskDependency> findByTaskIdAndDependencyType(Long taskId, DependencyType dependencyType);

    Optional<TaskDependency> findByTaskIdAndDependsOnTaskIdAndDependencyType(
            Long taskId, Long dependsOnTaskId, DependencyType dependencyType);

    boolean existsByTaskIdAndDependsOnTaskIdAndDependencyType(
            Long taskId, Long dependsOnTaskId, DependencyType dependencyType);

    @Query("SELECT td FROM TaskDependency td JOIN FETCH td.dependsOnTask WHERE td.task.id = :taskId")
    List<TaskDependency> findDependenciesWithDetails(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskDependency td WHERE td.task.id = :taskId OR td.dependsOnTask.id = :taskId")
    void deleteAllForTask(@Param("taskId") Long taskId);
}
