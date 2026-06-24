package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {

    List<TaskList> findByTaskBundleIdOrderByDisplayOrderAsc(Long taskBundleId);

    @Query("SELECT tl FROM TaskList tl WHERE tl.taskBundle.id = :bundleId ORDER BY tl.displayOrder ASC")
    List<TaskList> findAllByBundleIdSorted(@Param("bundleId") Long bundleId);

    @Query("SELECT tl FROM TaskList tl JOIN FETCH tl.taskBundle WHERE tl.id = :id")
    Optional<TaskList> findByIdWithBundle(@Param("id") Long id);

    boolean existsByNameAndTaskBundleId(String name, Long taskBundleId);

    @Query("SELECT COALESCE(MAX(tl.displayOrder), 0) FROM TaskList tl WHERE tl.taskBundle.id = :bundleId")
    Integer getMaxDisplayOrderByBundleId(@Param("bundleId") Long bundleId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.taskList.id = :listId AND t.isDeleted = false")
    Long countActiveTasksByListId(@Param("listId") Long listId);
}
