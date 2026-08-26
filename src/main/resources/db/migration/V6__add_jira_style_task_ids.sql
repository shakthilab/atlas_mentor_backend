-- ============================================================
-- V6__add_jira_style_task_ids.sql
--
-- Add Jira-style human-readable Task IDs per role.
--
-- Example:
--   ADM-1
--   ADM-2
--   SC-1
--   SC-2
--   JC-1
--   JC-2
--
-- This migration is designed to work with an existing
-- role_task_counters table where `id` may already exist
-- as the primary key.
-- ============================================================


-- ============================================================
-- 1. ADD ROLE CODE
-- ============================================================

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS code VARCHAR(10);


-- ============================================================
-- 2. POPULATE ROLE CODES
-- ============================================================

UPDATE roles
SET code = 'ADM'
WHERE name = 'ADMIN';

UPDATE roles
SET code = 'MGR'
WHERE name = 'MANAGER';

UPDATE roles
SET code = 'AA'
WHERE name = 'ADMINISTRATIVE_ASSISTANT';

UPDATE roles
SET code = 'BP'
WHERE name = 'BRANCH_PARTNER';

UPDATE roles
SET code = 'VE'
WHERE name = 'VIDEO_EDITOR';

UPDATE roles
SET code = 'SC'
WHERE name = 'SENIOR_COUNSELLOR';

UPDATE roles
SET code = 'JC'
WHERE name = 'JUNIOR_COUNSELLOR';

UPDATE roles
SET code = 'CN'
WHERE name = 'COUNSELLOR';

UPDATE roles
SET code = 'RF'
WHERE name = 'REFERRAL';

UPDATE roles
SET code = 'CP'
WHERE name = 'COMPANY';


-- ============================================================
-- 3. CREATE role_task_counters IF IT DOES NOT EXIST
--
-- IMPORTANT:
-- Existing production DB already has this table.
-- CREATE TABLE IF NOT EXISTS will NOT modify it.
-- ============================================================

CREATE TABLE IF NOT EXISTS role_task_counters (
                                                  role_id BIGINT NOT NULL,
                                                  last_sequence_number BIGINT NOT NULL DEFAULT 0
);


-- ============================================================
-- 4. ENSURE role_id IS UNIQUE
--
-- One counter must exist per role.
--
-- Existing DB currently has:
--     id       PRIMARY KEY
--     role_id  FOREIGN KEY
--
-- Therefore ON CONFLICT(role_id) would fail unless we
-- explicitly create a unique constraint/index on role_id.
-- ============================================================

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_task_counters_role_id
    ON role_task_counters(role_id);


-- ============================================================
-- 5. CREATE COUNTER ROWS FOR ALL ROLES WITH A CODE
-- ============================================================

INSERT INTO role_task_counters (
    role_id,
    last_sequence_number
)
SELECT
    r.id,
    0
FROM roles r
WHERE r.code IS NOT NULL
    ON CONFLICT (role_id) DO NOTHING;


-- ============================================================
-- 6. ADD display_id TO TASKS
-- ============================================================

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS display_id VARCHAR(20);


-- ============================================================
-- 7. BACKFILL EXISTING TASKS
--
-- IMPORTANT:
-- Bundle tasks and manually-created tasks are combined into
-- ONE sequence per role.
--
-- This prevents:
--
--     SC-1
--     SC-2
--     SC-3
--
-- and then manual tasks also becoming:
--
--     SC-1
--     SC-2
--
-- Instead they become:
--
--     SC-1
--     SC-2
--     SC-3
--     SC-4
--     SC-5
--
-- task ID ordering:
--     created_at ASC
--     task ID ASC
-- ============================================================

WITH task_roles AS (

    -- --------------------------------------------------------
    -- Tasks generated from task bundles
    -- --------------------------------------------------------

    SELECT
        t.id AS task_id,
        tb.role_id AS role_id,
        t.created_at AS created_at
    FROM tasks t
             INNER JOIN task_bundles tb
                        ON t.task_bundle_id = tb.id
    WHERE t.display_id IS NULL


    UNION ALL


    -- --------------------------------------------------------
    -- Manually created tasks
    -- Role is determined from assigned user
    -- --------------------------------------------------------

    SELECT
        t.id AS task_id,
        u.role_id AS role_id,
        t.created_at AS created_at
    FROM tasks t
             INNER JOIN users u
                        ON t.assigned_to = u.id
    WHERE t.display_id IS NULL
      AND t.task_bundle_id IS NULL
),

     ordered_tasks AS (

         SELECT
             tr.task_id,
             tr.role_id,
             r.code,

             ROW_NUMBER() OVER (
            PARTITION BY tr.role_id
            ORDER BY
                tr.created_at ASC,
                tr.task_id ASC
        ) AS seq_num

         FROM task_roles tr

                  INNER JOIN roles r
                             ON r.id = tr.role_id

         WHERE r.code IS NOT NULL
     )

UPDATE tasks t
SET display_id = ot.code || '-' || ot.seq_num
    FROM ordered_tasks ot
WHERE t.id = ot.task_id;


-- ============================================================
-- 8. CREATE UNIQUE INDEX FOR display_id
--
-- This is intentionally done AFTER the backfill.
-- ============================================================

CREATE UNIQUE INDEX IF NOT EXISTS uk_tasks_display_id
    ON tasks(display_id);


-- ============================================================
-- 9. UPDATE role_task_counters
--
-- Set each role's counter to the highest sequence number
-- currently assigned to that role.
-- ============================================================

UPDATE role_task_counters rtc
SET last_sequence_number = COALESCE(
        (
            SELECT MAX(seq_num)
            FROM (
                     SELECT
                         CAST(
                                 SPLIT_PART(t.display_id, '-', 2)
                             AS BIGINT
                         ) AS seq_num

                     FROM tasks t

                              INNER JOIN task_bundles tb
                                         ON t.task_bundle_id = tb.id

                     WHERE tb.role_id = rtc.role_id
                       AND t.display_id IS NOT NULL


                     UNION ALL


                     SELECT
                         CAST(
                                 SPLIT_PART(t.display_id, '-', 2)
                             AS BIGINT
                         ) AS seq_num

                     FROM tasks t

                              INNER JOIN users u
                                         ON t.assigned_to = u.id

                     WHERE u.role_id = rtc.role_id
                       AND t.task_bundle_id IS NULL
                       AND t.display_id IS NOT NULL

                 ) sequences
        ),
        0
                           );


-- ============================================================
-- 10. ENSURE COUNTERS EXIST FOR ANY NEW/REMAINING ROLES
-- ============================================================

INSERT INTO role_task_counters (
    role_id,
    last_sequence_number
)
SELECT
    r.id,
    0
FROM roles r
WHERE r.code IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM role_task_counters rtc
    WHERE rtc.role_id = r.id
)
    ON CONFLICT (role_id) DO NOTHING;


-- ============================================================
-- MIGRATION COMPLETE
-- ============================================================