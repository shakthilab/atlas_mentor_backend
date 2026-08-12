-- ============================================================
-- V13__fix_day_approvals_stage_constraint.sql
-- Fix a live/migration-file drift discovered while end-to-end testing the
-- approval workflow: the actual day_approvals.stage CHECK constraint in the
-- database allows ('BRANCH_PARTNER','BRANCH_MANAGER','ADMIN') - not
-- ('PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED') as V8's own file
-- content says it should. Every POST /api/days/{id}/approve call failed
-- with a 409 (DataIntegrityViolationException) writing the day_approvals
-- row as a result.
--
-- 'BRANCH_MANAGER' in particular doesn't match any real role name in this
-- system (the role is called 'MANAGER', not 'BRANCH_MANAGER') - a strong
-- signal this is stale/orphaned schema from an earlier draft of the
-- feature, never actually exercised by a working code path. Standardizing
-- on PARTNER_REVIEW/MANAGER_REVIEW/ADMIN_VERIFIED here because it's the
-- vocabulary day_workspaces.approval_stage already uses (see V7/V8) and
-- what DayApprovalService is built against - keeps both tables speaking
-- the same stage vocabulary instead of two different ones.
--
-- Also re-asserts the tasks.reflect_stage / reflect_state CHECK
-- constraints from V12, which exist in that file but were found missing
-- from the live database when this was written (see the same
-- migration-file-vs-live-schema drift note below).
--
-- ============================================================
-- IMPORTANT - if you are reading this because you apply migrations by
-- hand: this project's flyway_schema_history table does not exist in at
-- least one live environment, meaning Flyway has never actually run
-- there and every V*.sql file only takes effect when someone executes it
-- directly against Postgres. That gap is why file content and the live
-- schema drifted apart in the first place (this file exists to close one
-- instance of it) - worth fixing at the infrastructure level so this
-- class of bug stops recurring: confirm spring.flyway.enabled is really
-- taking effect against every environment these files are meant to apply to.
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
        WHERE rel.relname = 'day_approvals'
          AND att.attname = 'stage'
          AND pgc.contype = 'c'
    LOOP
        EXECUTE format('ALTER TABLE day_approvals DROP CONSTRAINT %I', con.conname);
    END LOOP;
END $$;

ALTER TABLE day_approvals
    ADD CONSTRAINT chk_day_approvals_stage
    CHECK (stage IN ('PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED'));

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS chk_tasks_reflect_stage;
ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_reflect_stage
    CHECK (reflect_stage IS NULL OR reflect_stage IN ('PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED'));

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS chk_tasks_reflect_state;
ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_reflect_state
    CHECK (reflect_state IS NULL OR reflect_state IN ('FLAGGED','RESUBMITTED'));
