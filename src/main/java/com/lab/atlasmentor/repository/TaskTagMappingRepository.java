package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTagMappingRepository extends JpaRepository<TaskTagMapping, Long> {

    List<TaskTagMapping> findByTaskId(Long taskId);

    List<TaskTagMapping> findByTagId(Long tagId);

    Optional<TaskTagMapping> findByTaskIdAndTagId(Long taskId, Long tagId);

    boolean existsByTaskIdAndTagId(Long taskId, Long tagId);

    @Modifying
    @Query("DELETE FROM TaskTagMapping ttm WHERE ttm.task.id = :taskId AND ttm.tag.id = :tagId")
    void deleteByTaskIdAndTagId(@Param("taskId") Long taskId, @Param("tagId") Long tagId);

    @Modifying
    @Query("DELETE FROM TaskTagMapping ttm WHERE ttm.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(ttm) FROM TaskTagMapping ttm WHERE ttm.tag.id = :tagId")
    Long countByTagId(@Param("tagId") Long tagId);
}
