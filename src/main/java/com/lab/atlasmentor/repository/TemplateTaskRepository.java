package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateTaskRepository extends JpaRepository<TemplateTask, Long> {
    List<TemplateTask> findByTemplateDayId(Long templateDayId);

    /**
     * Direct bulk delete by ID. Entity-based delete(entity)/deleteById(id) is unreliable for
     * TemplateTask when it was loaded via TemplateDay.templateTasks (a cascade=ALL,
     * orphanRemoval=true collection) — it silently emits no DELETE statement at all. This
     * bulk JPQL delete always issues a real DELETE and should be used instead.
     */
    @Modifying
    @Query("delete from TemplateTask t where t.id = :id")
    int deleteByTaskId(Long id);

    /**
     * Bulk delete of every task on one day, for the same reason as {@link #deleteByTaskId}:
     * entity-based delete is unreliable once tasks are loaded via TemplateDay.templateTasks.
     */
    @Modifying
    @Query("delete from TemplateTask t where t.templateDay.id = :templateDayId")
    int deleteByTemplateDayId(Long templateDayId);
}
