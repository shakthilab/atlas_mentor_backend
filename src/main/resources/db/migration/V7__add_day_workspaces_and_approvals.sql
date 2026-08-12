-- ============================================================
-- V7__add_day_workspaces_and_approvals.sql
-- Add day_workspaces and day_approvals tables
-- ============================================================

-- ============================================================
-- 1. CREATE day_workspaces TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS day_workspaces (
    id                        BIGSERIAL PRIMARY KEY,
    approval_stage            VARCHAR(50)  NOT NULL,
    daily_completion_pct      NUMERIC(5,2),
    day_number_in_month       INTEGER,
    is_weekly_checkpoint_day  BOOLEAN      NOT NULL DEFAULT FALSE,
    streak_count              INTEGER      NOT NULL DEFAULT 0,
    work_date                 DATE         NOT NULL,
    branch_id                 BIGINT,
    employee_user_id          BIGINT       NOT NULL,
    created_at                TIMESTAMP,
    updated_at                TIMESTAMP,
    created_by                BIGINT,
    updated_by                BIGINT,
    CONSTRAINT fk_day_workspaces_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL,
    CONSTRAINT fk_day_workspaces_employee
        FOREIGN KEY (employee_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_day_workspaces_employee ON day_workspaces(employee_user_id);
CREATE INDEX IF NOT EXISTS idx_day_workspaces_branch   ON day_workspaces(branch_id);
CREATE INDEX IF NOT EXISTS idx_day_workspaces_date    ON day_workspaces(work_date);

-- ============================================================
-- 2. CREATE day_approvals TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS day_approvals (
    id                BIGSERIAL PRIMARY KEY,
    acted_at          TIMESTAMP    NOT NULL,
    action            VARCHAR(50)  NOT NULL,
    comment           TEXT,
    stage             VARCHAR(50)  NOT NULL,
    approver_user_id  BIGINT       NOT NULL,
    day_workspace_id  BIGINT       NOT NULL,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    CONSTRAINT fk_day_approvals_approver
        FOREIGN KEY (approver_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_day_approvals_day_workspace
        FOREIGN KEY (day_workspace_id) REFERENCES day_workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_day_approvals_workspace ON day_approvals(day_workspace_id);
CREATE INDEX IF NOT EXISTS idx_day_approvals_approver  ON day_approvals(approver_user_id);
