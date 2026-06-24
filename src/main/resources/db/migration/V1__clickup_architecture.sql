-- ============================================================
-- V1__clickup_architecture.sql
-- ClickUp-style architecture extension for Atlas Mentor
--
-- Idempotent: uses IF NOT EXISTS / ADD COLUMN IF NOT EXISTS
-- Safe to run on existing databases (baseline-on-migrate=true)
-- ============================================================

-- ============================================================
-- 1. EXTEND task_bundles TABLE
--    Add: color, icon, bundle_branch_id, display_order
-- ============================================================
ALTER TABLE task_bundles
    ADD COLUMN IF NOT EXISTS color         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS icon          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bundle_branch_id BIGINT,
    ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

ALTER TABLE task_bundles
    ADD CONSTRAINT IF NOT EXISTS fk_task_bundles_branch
        FOREIGN KEY (bundle_branch_id) REFERENCES branches(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_task_bundles_branch_id ON task_bundles(bundle_branch_id);
CREATE INDEX IF NOT EXISTS idx_task_bundles_display_order ON task_bundles(display_order);

-- ============================================================
-- 2. CREATE task_lists TABLE
--    Equivalent to ClickUp List (inside a Folder/Bundle)
-- ============================================================
CREATE TABLE IF NOT EXISTS task_lists (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    color           VARCHAR(20),
    display_order   INTEGER      DEFAULT 0,
    task_bundle_id  BIGINT       NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT fk_task_lists_bundle
        FOREIGN KEY (task_bundle_id) REFERENCES task_bundles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_task_lists_bundle_id     ON task_lists(task_bundle_id);
CREATE INDEX IF NOT EXISTS idx_task_lists_display_order ON task_lists(display_order);

-- ============================================================
-- 3. EXTEND tasks TABLE
--    Add: task_list_id, parent_task_id, start_date,
--         estimated_minutes, actual_minutes
-- ============================================================

-- Extend TaskStatus enum with new board-view values
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum
                   WHERE enumlabel = 'TODO'
                     AND enumtypid = 'task_status'::regtype) THEN
        ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'TODO';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum
                   WHERE enumlabel = 'REVIEW'
                     AND enumtypid = 'task_status'::regtype) THEN
        ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'REVIEW';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum
                   WHERE enumlabel = 'DONE'
                     AND enumtypid = 'task_status'::regtype) THEN
        ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'DONE';
    END IF;
END $$;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS task_list_id      BIGINT,
    ADD COLUMN IF NOT EXISTS parent_task_id    BIGINT,
    ADD COLUMN IF NOT EXISTS start_date        DATE,
    ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS actual_minutes    INTEGER;

ALTER TABLE tasks
    ADD CONSTRAINT IF NOT EXISTS fk_tasks_task_list
        FOREIGN KEY (task_list_id) REFERENCES task_lists(id) ON DELETE SET NULL;

ALTER TABLE tasks
    ADD CONSTRAINT IF NOT EXISTS fk_tasks_parent_task
        FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_task_list_id   ON tasks(task_list_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_task_id ON tasks(parent_task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_start_date     ON tasks(start_date);

-- ============================================================
-- 4. CREATE user_reporting TABLE
--    Dynamic role hierarchy for branch-scoped reporting
-- ============================================================
CREATE TABLE IF NOT EXISTS user_reporting (
    id               BIGSERIAL PRIMARY KEY,
    manager_user_id  BIGINT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    branch_id        BIGINT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       BIGINT,
    updated_by       BIGINT,
    CONSTRAINT fk_user_reporting_manager
        FOREIGN KEY (manager_user_id)  REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_reporting_employee
        FOREIGN KEY (employee_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_reporting_branch
        FOREIGN KEY (branch_id)        REFERENCES branches(id) ON DELETE SET NULL,
    CONSTRAINT uk_user_reporting_manager_employee
        UNIQUE (manager_user_id, employee_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_reporting_manager  ON user_reporting(manager_user_id);
CREATE INDEX IF NOT EXISTS idx_user_reporting_employee ON user_reporting(employee_user_id);
CREATE INDEX IF NOT EXISTS idx_user_reporting_branch   ON user_reporting(branch_id);

-- ============================================================
-- 5. CREATE task_assignments TABLE
--    Multiple assignees per task
-- ============================================================
CREATE TABLE IF NOT EXISTS task_assignments (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT fk_task_assignments_task
        FOREIGN KEY (task_id)     REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignments_user
        FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignments_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_task_assignments_task_user
        UNIQUE (task_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_task_assignments_task_id     ON task_assignments(task_id);
CREATE INDEX IF NOT EXISTS idx_task_assignments_user_id     ON task_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_task_assignments_assigned_by ON task_assignments(assigned_by);

-- ============================================================
-- 6. CREATE task_watchers TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS task_watchers (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_task_watchers_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_watchers_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_task_watchers_task_user
        UNIQUE (task_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_task_watchers_task_id ON task_watchers(task_id);
CREATE INDEX IF NOT EXISTS idx_task_watchers_user_id ON task_watchers(user_id);

-- ============================================================
-- 7. CREATE task_dependencies TABLE
--    Supports BLOCKS / WAITING_FOR / RELATED
-- ============================================================
CREATE TABLE IF NOT EXISTS task_dependencies (
    id                BIGSERIAL PRIMARY KEY,
    task_id           BIGINT      NOT NULL,
    depends_on_task_id BIGINT     NOT NULL,
    dependency_type   VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    CONSTRAINT fk_task_deps_task
        FOREIGN KEY (task_id)            REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_deps_depends_on
        FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uk_task_dependencies_unique
        UNIQUE (task_id, depends_on_task_id, dependency_type),
    CONSTRAINT chk_task_deps_no_self
        CHECK (task_id <> depends_on_task_id)
);

CREATE INDEX IF NOT EXISTS idx_task_deps_task_id     ON task_dependencies(task_id);
CREATE INDEX IF NOT EXISTS idx_task_deps_depends_on  ON task_dependencies(depends_on_task_id);
CREATE INDEX IF NOT EXISTS idx_task_deps_type        ON task_dependencies(dependency_type);

-- ============================================================
-- 8. CREATE task_attachments TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS task_attachments (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT        NOT NULL,
    file_name   VARCHAR(255)  NOT NULL,
    file_url    VARCHAR(1000) NOT NULL,
    file_size   BIGINT,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT fk_task_attachments_task
        FOREIGN KEY (task_id)     REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachments_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_task_attachments_task_id     ON task_attachments(task_id);
CREATE INDEX IF NOT EXISTS idx_task_attachments_uploaded_by ON task_attachments(uploaded_by);

-- ============================================================
-- 9. CREATE task_tags TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS task_tags (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    color      VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_task_tags_name ON task_tags(name);

-- ============================================================
-- 10. CREATE task_tag_mapping TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS task_tag_mapping (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_task_tag_mapping_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tag_mapping_tag
        FOREIGN KEY (tag_id)  REFERENCES task_tags(id) ON DELETE CASCADE,
    CONSTRAINT uk_task_tag_mapping_task_tag
        UNIQUE (task_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_task_tag_mapping_task_id ON task_tag_mapping(task_id);
CREATE INDEX IF NOT EXISTS idx_task_tag_mapping_tag_id  ON task_tag_mapping(tag_id);

-- ============================================================
-- 11. CREATE notifications TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    reference_id BIGINT,
    title        VARCHAR(200),
    message      TEXT,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   BIGINT,
    updated_by   BIGINT,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id   ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read   ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_type      ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================================
-- 12. CREATE recurring_tasks TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS recurring_tasks (
    id                   BIGSERIAL PRIMARY KEY,
    task_id              BIGINT      NOT NULL UNIQUE,
    frequency            VARCHAR(20) NOT NULL,
    interval_value       INTEGER     DEFAULT 1,
    next_execution_time  TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    created_by           BIGINT,
    updated_by           BIGINT,
    CONSTRAINT fk_recurring_tasks_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recurring_tasks_task_id        ON recurring_tasks(task_id);
CREATE INDEX IF NOT EXISTS idx_recurring_tasks_next_execution ON recurring_tasks(next_execution_time);
CREATE INDEX IF NOT EXISTS idx_recurring_tasks_frequency      ON recurring_tasks(frequency);

-- ============================================================
-- 13. CREATE time_tracking TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS time_tracking (
    id                  BIGSERIAL PRIMARY KEY,
    task_id             BIGINT    NOT NULL,
    user_id             BIGINT    NOT NULL,
    start_time          TIMESTAMP NOT NULL,
    end_time            TIMESTAMP,
    duration_in_seconds BIGINT,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT fk_time_tracking_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_tracking_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_time_tracking_task_id   ON time_tracking(task_id);
CREATE INDEX IF NOT EXISTS idx_time_tracking_user_id   ON time_tracking(user_id);
CREATE INDEX IF NOT EXISTS idx_time_tracking_start_time ON time_tracking(start_time);
