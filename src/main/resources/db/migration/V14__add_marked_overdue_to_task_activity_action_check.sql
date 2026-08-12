-- ============================================================
-- V14__add_marked_overdue_to_task_activity_action_check.sql
-- task_activity.action's live CHECK constraint only allows
-- ('CREATED','STATUS_CHANGED','PRIORITY_CHANGED','ASSIGNED','COMMENT_ADDED',
-- 'ATTACHMENT_ADDED','DUE_DATE_UPDATED') - it was never extended when
-- TaskAction.MARKED_OVERDUE was added to the Java enum and wired up in
-- OverdueTaskSchedulerService#markTaskAsOverdue.
--
-- Effect in production: every hourly run of OverdueTaskSchedulerService
-- sets the task's status to OVERDUE, then fails to insert its activity
-- row (constraint violation) and the whole per-task @Transactional call
-- rolls back - taking the status change down with it. The scheduler
-- silently no-ops forever. Meanwhile the duplicate, since-removed
-- OverdueTaskService ran the same job with its own DB-legal
-- ('STATUS_CHANGED') activity write and a `dueDate <= today` (not `<`)
-- comparison, so it "worked" but marked tasks overdue the moment the
-- scheduler ticked past midnight on their due date - hours before the
-- task's actual due time, and even for tasks created that same morning
-- via template instantiation (see TemplateInstantiationService, which
-- sets dueDate = today). That's the bug reported: a task due 6:00 PM
-- today was already OVERDUE at 07:35 the same day.
--
-- Fix here is schema-only: allow MARKED_OVERDUE so
-- OverdueTaskSchedulerService (dueDate < today, i.e. a task stays
-- non-overdue through 23:59 on its due date) can actually persist.
-- ============================================================

DO $$
DECLARE
    con RECORD;
BEGIN
    FOR con IN
        SELECT pgc.conname
        FROM pg_constraint pgc
        JOIN pg_class rel ON rel.oid = pgc.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(pgc.conkey)
        WHERE rel.relname = 'task_activity'
          AND att.attname = 'action'
          AND pgc.contype = 'c'
    LOOP
        EXECUTE format('ALTER TABLE task_activity DROP CONSTRAINT %I', con.conname);
    END LOOP;
END $$;

ALTER TABLE task_activity
    ADD CONSTRAINT task_activity_action_check
    CHECK (action IN (
        'CREATED','STATUS_CHANGED','PRIORITY_CHANGED','ASSIGNED','COMMENT_ADDED',
        'ATTACHMENT_ADDED','DUE_DATE_UPDATED','MARKED_OVERDUE'
    ));
