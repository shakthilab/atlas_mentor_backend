-- ============================================================
-- V32__remove_pending_task_status.sql
-- Retire PENDING as a task status.
--
-- PENDING and TODO both meant "not started", but two separate creation paths
-- (Task's own constructors/field default vs. TemplateInstantiationService) had
-- drifted onto different values for the same state, and TaskService#validateStatusTransition
-- carried a second, stricter transition rule just for PENDING (PENDING -> IN_PROGRESS only,
-- vs. TODO's PENDING/TODO -> IN_PROGRESS or straight to DONE). Every task now starts at
-- TODO and follows the single TODO-first flow - see that method's javadoc.
--
-- Mirrors V11/V17's dual-shape handling (native enum type vs. plain column + CHECK
-- constraint) - see V11's comment for why both paths are needed.
-- ============================================================

-- Fold any existing PENDING rows into TODO before narrowing the constraint below, so
-- nothing is left holding a value the new CHECK will reject.
UPDATE tasks SET status = 'TODO' WHERE status = 'PENDING';

DO $$
DECLARE
    con RECORD;
BEGIN
    IF to_regtype('task_status') IS NOT NULL THEN
        -- Native enum path: Postgres has no DROP VALUE for enum types, so PENDING stays
        -- defined at the type level as an unused label - the application no longer writes
        -- it (TaskStatus.PENDING removed from the Java enum) and no row can still hold it
        -- (UPDATE above already folded them into TODO).
        NULL;
    ELSE
        FOR con IN
            SELECT pgc.conname
            FROM pg_constraint pgc
            JOIN pg_class rel ON rel.oid = pgc.conrelid
            JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(pgc.conkey)
            WHERE rel.relname = 'tasks'
              AND att.attname = 'status'
              AND pgc.contype = 'c'
        LOOP
            EXECUTE format('ALTER TABLE tasks DROP CONSTRAINT %I', con.conname);
        END LOOP;

        ALTER TABLE tasks
            ADD CONSTRAINT tasks_status_check
            CHECK (status IN ('TODO','IN_PROGRESS','REVIEW','DONE','COMPLETED',
                               'OVERDUE','CANCELLED','REFLECT','VERIFIED'));
    END IF;
END $$;
