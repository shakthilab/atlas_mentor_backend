-- ============================================================
-- V23__add_task_completed_at.sql
-- Add completed_at to tasks: the exact moment a task's status became DONE,
-- independent of due_date (which never changes once a task is created - see
-- the Overdue Task Rollover spec). Gives a direct, queryable "when was this
-- actually done" answer instead of requiring task_activity parsing, and lets
-- "originally due Aug 16, actually completed Aug 18" be read straight off
-- one row.
--
-- Nullable: unset for every task that hasn't reached DONE yet. Set exactly
-- once a task's status transitions to DONE (TaskService#updateTaskStatus),
-- regardless of how many days past due_date that happens. due_date and
-- day_workspace_id are never touched by this - a carried-over overdue task
-- completed late stays attributed to its original day (see
-- DayApprovalService/EmployeeTreeService), this column only records when the
-- work itself actually finished.
-- ============================================================

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- Backfill: any task already sitting at DONE/COMPLETED/VERIFIED has, by definition,
-- been completed - approximate the timestamp from updated_at (the closest existing
-- signal) rather than leaving historical rows null forever.
UPDATE tasks
SET completed_at = updated_at
WHERE status IN ('DONE', 'COMPLETED', 'VERIFIED')
  AND completed_at IS NULL;
