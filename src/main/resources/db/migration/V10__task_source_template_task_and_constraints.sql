-- ============================================================
-- V10__task_source_template_task_and_constraints.sql
-- Track which TemplateTask a Task was generated from, and use that to
-- make template-driven task instantiation idempotent at the DB level -
-- closing the check-then-create race between the nightly cron, the
-- startup catch-up check, the periodic safety-net check, and Publish.
-- ============================================================

-- ============================================================
-- 1. day_workspaces(employee_user_id, work_date) UNIQUE constraint
--    Already added by V8__day_workspace_constraints.sql. Re-asserted
--    here, idempotently, in case this migration runs against an
--    environment where V8 was skipped.
-- ============================================================
ALTER TABLE day_workspaces
    DROP CONSTRAINT IF EXISTS uk_day_workspaces_employee_date;

ALTER TABLE day_workspaces
    ADD CONSTRAINT uk_day_workspaces_employee_date
    UNIQUE (employee_user_id, work_date);

-- ============================================================
-- 2. tasks.source_template_task_id - which TemplateTask (if any) this
--    task was instantiated from. NULL for manual/non-template tasks.
-- ============================================================
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS source_template_task_id BIGINT;

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS fk_tasks_source_template_task;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_source_template_task
    FOREIGN KEY (source_template_task_id) REFERENCES template_tasks(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_source_template_task ON tasks(source_template_task_id);

-- ============================================================
-- 3. UNIQUENESS - one task per (day_workspace, source template task).
--    NULLs are never considered equal by a UNIQUE constraint, so manual
--    tasks (source_template_task_id IS NULL, day_workspace_id often
--    NULL too) are completely unaffected - this only guards
--    template-generated tasks against being duplicated by two
--    triggers racing on the same employee's same day.
-- ============================================================
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS uk_tasks_day_workspace_source_template_task;

ALTER TABLE tasks
    ADD CONSTRAINT uk_tasks_day_workspace_source_template_task
    UNIQUE (day_workspace_id, source_template_task_id);
