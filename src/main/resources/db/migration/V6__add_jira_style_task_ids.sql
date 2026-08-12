-- ============================================================
-- V6__add_jira_style_task_ids.sql
-- Add Jira-style human-readable Task IDs (per role)
-- ============================================================

-- ============================================================
-- 1. ADD code COLUMN TO roles TABLE
-- ============================================================
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS code VARCHAR(10);

-- ============================================================
-- 2. POPULATE role codes
-- ============================================================
UPDATE roles SET code = 'ADM' WHERE name = 'ADMIN';
UPDATE roles SET code = 'MGR' WHERE name = 'MANAGER';
UPDATE roles SET code = 'AA' WHERE name = 'ADMINISTRATIVE_ASSISTANT';
UPDATE roles SET code = 'BP' WHERE name = 'BRANCH_PARTNER';
UPDATE roles SET code = 'VE' WHERE name = 'VIDEO_EDITOR';
UPDATE roles SET code = 'SC' WHERE name = 'SENIOR_COUNSELLOR';
UPDATE roles SET code = 'JC' WHERE name = 'JUNIOR_COUNSELLOR';
UPDATE roles SET code = 'CN' WHERE name = 'COUNSELLOR';
UPDATE roles SET code = 'RF' WHERE name = 'REFERRAL';
UPDATE roles SET code = 'CP' WHERE name = 'COMPANY';
-- STUDENT gets null (no tasks)

-- ============================================================
-- 3. CREATE role_task_counters TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS role_task_counters (
    role_id             BIGINT PRIMARY KEY REFERENCES roles(id) ON DELETE CASCADE,
    last_sequence_number BIGINT NOT NULL DEFAULT 0
);

-- ============================================================
-- 4. ADD display_id COLUMN TO tasks TABLE
-- ============================================================
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS display_id VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tasks_display_id ON tasks(display_id);

-- ============================================================
-- 5. BACKFILL EXISTING TASKS WITH display_id
--    Assign display IDs in creation order (oldest first) per role
-- ============================================================
-- First, initialize counters for all roles that might have tasks
INSERT INTO role_task_counters (role_id, last_sequence_number)
SELECT DISTINCT r.id, 0
FROM roles r
WHERE r.code IS NOT NULL
ON CONFLICT (role_id) DO NOTHING;

-- Backfill tasks with task_bundles (template-generated tasks)
WITH ordered_bundle_tasks AS (
    SELECT
        t.id,
        tb.role_id,
        r.code,
        t.created_at,
        ROW_NUMBER() OVER (PARTITION BY tb.role_id ORDER BY t.created_at ASC) as seq_num
    FROM tasks t
    INNER JOIN task_bundles tb ON t.task_bundle_id = tb.id
    INNER JOIN roles r ON tb.role_id = r.id
    WHERE t.display_id IS NULL
    AND r.code IS NOT NULL
)
UPDATE tasks t
SET display_id = obt.code || '-' || obt.seq_num
FROM ordered_bundle_tasks obt
WHERE t.id = obt.id;

-- Backfill tasks without task_bundles (manually created tasks) - use assigned user's role
WITH ordered_manual_tasks AS (
    SELECT
        t.id,
        u.role_id,
        r.code,
        t.created_at,
        ROW_NUMBER() OVER (PARTITION BY u.role_id ORDER BY t.created_at ASC) as seq_num
    FROM tasks t
    INNER JOIN users u ON t.assigned_to = u.id
    INNER JOIN roles r ON u.role_id = r.id
    WHERE t.display_id IS NULL
    AND t.task_bundle_id IS NULL
    AND r.code IS NOT NULL
)
UPDATE tasks t
SET display_id = omt.code || '-' || omt.seq_num
FROM ordered_manual_tasks omt
WHERE t.id = omt.id;

-- Update the role_task_counters with the highest sequence number used
UPDATE role_task_counters rtc
SET last_sequence_number = (
    SELECT COALESCE(MAX(seq_num), 0)
    FROM (
        SELECT
            CAST(SPLIT_PART(t.display_id, '-', 2) AS BIGINT) as seq_num
        FROM tasks t
        INNER JOIN task_bundles tb ON t.task_bundle_id = tb.id
        WHERE tb.role_id = rtc.role_id
        AND t.display_id IS NOT NULL
        UNION ALL
        SELECT
            CAST(SPLIT_PART(t.display_id, '-', 2) AS BIGINT) as seq_num
        FROM tasks t
        INNER JOIN users u ON t.assigned_to = u.id
        WHERE u.role_id = rtc.role_id
        AND t.task_bundle_id IS NULL
        AND t.display_id IS NOT NULL
    ) seqs
)
WHERE EXISTS (
    SELECT 1 FROM tasks t
    WHERE (
        (t.task_bundle_id IS NOT NULL AND EXISTS (
            SELECT 1 FROM task_bundles tb WHERE tb.id = t.task_bundle_id AND tb.role_id = rtc.role_id
        ))
        OR (t.task_bundle_id IS NULL AND EXISTS (
            SELECT 1 FROM users u WHERE u.id = t.assigned_to AND u.role_id = rtc.role_id
        ))
    )
    AND t.display_id IS NOT NULL
);
