package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {

    Optional<TaskTag> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT t FROM TaskTag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<TaskTag> searchByName(@Param("keyword") String keyword);

    @Query("SELECT tt FROM TaskTag tt WHERE tt.id IN " +
           "(SELECT ttm.tag.id FROM TaskTagMapping ttm WHERE ttm.task.id = :taskId)")
    List<TaskTag> findTagsByTaskId(@Param("taskId") Long taskId);
}
