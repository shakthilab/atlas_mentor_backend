-- ============================================================
-- V18__add_task_current_next_step.sql
-- Add current_step / next_step to tasks: a persisted, per-task view of where a task
-- sits in the Day Approval Workflow (see DayApprovalService), instead of that only
-- being derivable at the day level (DayDetailResponse#currentStep).
--
-- Outside of a send-back cycle, a task's current/next step mirrors its day
-- workspace's approval_stage (Not Submitted -> Submitted -> Branch Partner Review ->
-- Manager Review -> Verified). While a task is individually sent back (reflect_stage /
-- reflect_state, V12) its current/next step instead reflects that rework cycle
-- specifically - e.g. reflect_state = FLAGGED by ADMIN_VERIFIED shows current_step
-- "Reflect" / next_step "Admin Review", independent of every other task on the same
-- day. See DayApprovalService#applyStepLabels, the single place both columns are
-- computed and written.
--
-- Nullable, denormalized and app-maintained (like reflect_stage/reflect_state before
-- it) rather than a DB-level generated column, because the mapping depends on the
-- day workspace's approval_stage - a join, not a value on the tasks row itself.
-- ============================================================

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS current_step VARCHAR(40);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS next_step VARCHAR(40);

-- Backfill every existing row so "every task has a current/next step" holds retroactively,
-- not just for tasks touched after this migration. Mirrors
-- DayApprovalService#applyStepLabels exactly - keep the two in sync if that method changes.

-- 1) Open reflect cycles take priority over the day's own approval_stage.
UPDATE tasks t
SET current_step = 'Reflect',
    next_step = CASE t.reflect_stage
        WHEN 'PARTNER_REVIEW' THEN 'Branch Partner Review'
        WHEN 'MANAGER_REVIEW' THEN 'Manager Review'
        WHEN 'ADMIN_VERIFIED' THEN 'Admin Review'
        ELSE t.reflect_stage
    END
WHERE t.reflect_state = 'FLAGGED';

UPDATE tasks t
SET current_step = 'Awaiting ' || CASE t.reflect_stage
        WHEN 'PARTNER_REVIEW' THEN 'Branch Partner Review'
        WHEN 'MANAGER_REVIEW' THEN 'Manager Review'
        WHEN 'ADMIN_VERIFIED' THEN 'Admin Review'
        ELSE t.reflect_stage
    END,
    next_step = NULL
WHERE t.reflect_state = 'RESUBMITTED';

-- 2) Everything else mirrors its day workspace's approval_stage. Tasks with no
--    day_workspace_id (manual/legacy, never part of this pipeline) are left null/null.
UPDATE tasks t
SET current_step = CASE dw.approval_stage
        WHEN 'EMPLOYEE' THEN 'Not Submitted'
        WHEN 'COMPLETED' THEN 'Submitted'
        WHEN 'PARTNER_REVIEW' THEN 'Branch Partner Review'
        WHEN 'MANAGER_REVIEW' THEN 'Manager Review'
        WHEN 'ADMIN_VERIFIED' THEN 'Verified'
        ELSE 'Not Submitted'
    END,
    next_step = CASE dw.approval_stage
        WHEN 'EMPLOYEE' THEN 'Submitted'
        WHEN 'COMPLETED' THEN 'Branch Partner Review'
        WHEN 'PARTNER_REVIEW' THEN 'Manager Review'
        WHEN 'MANAGER_REVIEW' THEN 'Admin Review'
        WHEN 'ADMIN_VERIFIED' THEN NULL
        ELSE 'Submitted'
    END
FROM day_workspaces dw
WHERE t.day_workspace_id = dw.id
  AND (t.reflect_state IS NULL OR t.reflect_state NOT IN ('FLAGGED', 'RESUBMITTED'));
