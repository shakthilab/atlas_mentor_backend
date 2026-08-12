-- ============================================================
-- V11__add_reflect_task_status.sql
-- Add REFLECT as a valid task_status value.
--
-- Used by the Day Approval Workflow (SEND_BACK action, see DayApprovalService): when a
-- Branch Partner / Branch Manager / Admin sends a day back for rework, any task(s) they
-- flag move to REFLECT so the employee can see exactly what needs fixing, with the
-- reviewer's comment attached via day_approvals.
--
-- tasks.status turns out NOT to be backed by a native Postgres enum type in every
-- environment - V1__clickup_architecture.sql's `ALTER TYPE task_status ADD VALUE`
-- approach assumes a `task_status` enum type exists, but at least one environment has
-- tasks.status as a plain column guarded by a CHECK constraint instead (confirmed by a
-- `type "task_status" does not exist` / 42704 error attempting the enum path here).
-- This migration handles both shapes at runtime via to_regtype(), which returns NULL
-- instead of erroring when the type doesn't exist (unlike the ::regtype cast V1 uses).
-- ============================================================

DO $$
DECLARE
    con RECORD;
BEGIN
    IF to_regtype('task_status') IS NOT NULL THEN
        -- Native enum type path (mirrors V1's TODO/REVIEW/DONE additions).
        IF NOT EXISTS (SELECT 1 FROM pg_enum
                       WHERE enumlabel = 'REFLECT'
                         AND enumtypid = 'task_status'::regtype) THEN
            ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'REFLECT';
        END IF;
    ELSE
        -- Plain column + CHECK constraint path. Drop whichever CHECK constraint(s)
        -- currently guard tasks.status (name unknown/environment-dependent) and replace
        -- with one that includes every status value the app actually uses, including the
        -- new REFLECT plus CANCELLED (already referenced as an allowed value elsewhere in
        -- the codebase's tasks_status_check, per TemplateInstantiationService).
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
                               'OVERDUE','CANCELLED','REFLECT'));
    END IF;
END $$;
