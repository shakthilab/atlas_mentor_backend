-- ============================================================
-- V17__add_verified_task_status.sql
-- Add VERIFIED as a valid task_status value.
--
-- Splits "employee marked it done" from "every reviewer signed off" (Day Approval
-- Workflow, see DayApprovalService): DONE remains the employee's own claim of
-- completion - mutable, still subject to REFLECT/send-back. VERIFIED is the new
-- terminal state DayApprovalService#approveDayLevel stamps on a task once the day
-- reaches ADMIN_VERIFIED (Admin's final approval) and the task itself isn't sitting
-- mid re-review (reflect_state IS NULL). Deliberately not reusing the existing
-- COMPLETED value - COMPLETED already means "same as DONE, legacy manual-flow
-- tasks" everywhere in the codebase (see TaskService#validateStatusTransition,
-- EmployeeTreeService#DONE_STATUSES), and day_workspaces.approval_stage separately
-- already uses the literal string "COMPLETED" for the *opposite* end of the
-- pipeline (employee just submitted, before any review) - a third meaning on top
-- of those two would only compound the confusion this migration is meant to fix.
--
-- Mirrors V11's dual-shape handling (native enum type vs. plain column + CHECK
-- constraint) - see that migration's comment for why both paths are needed.
-- ============================================================

DO $$
DECLARE
    con RECORD;
BEGIN
    IF to_regtype('task_status') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM pg_enum
                       WHERE enumlabel = 'VERIFIED'
                         AND enumtypid = 'task_status'::regtype) THEN
            ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'VERIFIED';
        END IF;
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
            CHECK (status IN ('PENDING','TODO','IN_PROGRESS','REVIEW','DONE','COMPLETED',
                               'OVERDUE','CANCELLED','REFLECT','VERIFIED'));
    END IF;
END $$;