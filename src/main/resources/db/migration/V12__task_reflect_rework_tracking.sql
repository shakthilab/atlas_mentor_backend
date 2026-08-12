-- ============================================================
-- V12__task_reflect_rework_tracking.sql
-- Per-task send-back/rework tracking, so a REFLECT flag on one task:
--   1. survives independently of "today" (Needs Attention, cross-date)
--   2. remembers exactly which stage flagged it (routes resubmission
--      back to that same stage, not a full Partner->Manager->Admin restart)
--   3. remembers what status to restore on resubmit
--   4. can be re-reviewed by that stage without touching any other task
--      or the day's own approval_stage pipeline position
--
-- Deliberately modeled on the task, not the day: day_approvals stays a
-- day-level audit log (unchanged), but it has no task_id column, so it
-- cannot answer "which stage flagged THIS task" when a day has several
-- tasks flagged by different stages at different times. These columns
-- are that missing per-task state.
-- ============================================================

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS reflect_stage VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reflect_state VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reflect_comment TEXT,
    ADD COLUMN IF NOT EXISTS reflect_flagged_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reflect_flagged_by BIGINT,
    ADD COLUMN IF NOT EXISTS reflect_resubmitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reflect_previous_status VARCHAR(20);

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS fk_tasks_reflect_flagged_by;
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_reflect_flagged_by
    FOREIGN KEY (reflect_flagged_by) REFERENCES users(id) ON DELETE SET NULL;

-- PARTNER_REVIEW/MANAGER_REVIEW/ADMIN_VERIFIED - same three review-stage labels
-- day_approvals.stage already uses (see V8), reused here as "which stage owns
-- this task's reflect cycle" rather than "which stage the day is sitting at".
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS chk_tasks_reflect_stage;
ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_reflect_stage
    CHECK (reflect_stage IS NULL OR reflect_stage IN ('PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED'));

-- FLAGGED: task is in REFLECT, waiting on the employee to fix it.
-- RESUBMITTED: employee fixed it and moved status forward again; waiting on
--   reflect_stage's reviewer to re-check it (this is the "review item" the
--   resubmit endpoint creates - surfaced via GET /api/approvals/pending).
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS chk_tasks_reflect_state;
ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_reflect_state
    CHECK (reflect_state IS NULL OR reflect_state IN ('FLAGGED','RESUBMITTED'));

CREATE INDEX IF NOT EXISTS idx_tasks_reflect_state ON tasks(reflect_state);

-- task_activity.comment: the reviewer's send-back comment attached directly to the
-- per-task activity entry, not just inferable from a separate task_comments row.
-- Every other action-log table in this schema (task_comments, day_approvals,
-- task_approvals) already has a comment/text column; task_activity was the one
-- missing it.
ALTER TABLE task_activity
    ADD COLUMN IF NOT EXISTS comment TEXT;
