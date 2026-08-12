-- ============================================================
-- V8__day_workspace_constraints.sql
-- Add uniqueness and CHECK constraints to day_workspaces and day_approvals
-- ============================================================

-- ============================================================
-- 1. UNIQUENESS — one workspace per employee per day
-- ============================================================
ALTER TABLE day_workspaces
    DROP CONSTRAINT IF EXISTS uk_day_workspaces_employee_date;

ALTER TABLE day_workspaces
    ADD CONSTRAINT uk_day_workspaces_employee_date
    UNIQUE (employee_user_id, work_date);

-- ============================================================
-- 2. CHECK constraint on day_workspaces.approval_stage
-- ============================================================
ALTER TABLE day_workspaces
    DROP CONSTRAINT IF EXISTS chk_day_workspaces_stage;

ALTER TABLE day_workspaces
    ADD CONSTRAINT chk_day_workspaces_stage
    CHECK (approval_stage IN ('EMPLOYEE','COMPLETED','PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED'));

-- ============================================================
-- 3. CHECK constraint on day_approvals.stage
-- ============================================================
ALTER TABLE day_approvals
    DROP CONSTRAINT IF EXISTS chk_day_approvals_stage;

ALTER TABLE day_approvals
    ADD CONSTRAINT chk_day_approvals_stage
    CHECK (stage IN ('PARTNER_REVIEW','MANAGER_REVIEW','ADMIN_VERIFIED'));

-- ============================================================
-- 4. CHECK constraint on day_approvals.action
-- ============================================================
ALTER TABLE day_approvals
    DROP CONSTRAINT IF EXISTS chk_day_approvals_action;

ALTER TABLE day_approvals
    ADD CONSTRAINT chk_day_approvals_action
    CHECK (action IN ('APPROVE','SEND_BACK'));
