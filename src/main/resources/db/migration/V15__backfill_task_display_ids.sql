-- ============================================================
-- V15__backfill_task_display_ids.sql
-- V6 added display_id + role_task_counters, but nothing ever called the generator on task
-- creation, so every task since has display_id IS NULL. This backfills those tasks and
-- seeds role_task_counters so the app's atomic generator (TaskDisplayIdService via
-- RoleTaskCounterRepository#incrementAndGetNextSequence) continues the sequence cleanly.
--
-- Unlike V6's backfill (which windowed bundle-tasks and manual-tasks separately per role and
-- could hand out the same "CODE-1" to both groups), this assigns one role-scoped sequence
-- across ALL of a role's backfilled tasks together, so it can't collide with itself or with
-- uk_tasks_display_id. Safe to run more than once - it only ever touches display_id IS NULL.
-- ============================================================

WITH tasks_with_role AS (
    SELECT
        t.id,
        COALESCE(tb.role_id, u.role_id) AS role_id,
        t.created_at
    FROM tasks t
    LEFT JOIN task_bundles tb ON t.task_bundle_id = tb.id
    LEFT JOIN users u ON t.assigned_to = u.id
    WHERE t.display_id IS NULL
),
numbered AS (
    SELECT
        twr.id,
        r.code,
        COALESCE(rtc.last_sequence_number, 0)
            + ROW_NUMBER() OVER (PARTITION BY twr.role_id ORDER BY twr.created_at ASC, twr.id ASC) AS seq_num
    FROM tasks_with_role twr
    JOIN roles r ON r.id = twr.role_id
    LEFT JOIN role_task_counters rtc ON rtc.role_id = twr.role_id
    WHERE r.code IS NOT NULL AND r.code <> ''
)
UPDATE tasks t
SET display_id = n.code || '-' || n.seq_num
FROM numbered n
WHERE t.id = n.id;

-- Advance/seed each role's counter to the highest sequence number now in use, so the next
-- live INSERT ... ON CONFLICT ... RETURNING increment continues from here.
INSERT INTO role_task_counters (role_id, last_sequence_number)
SELECT COALESCE(tb.role_id, u.role_id) AS role_id, MAX(CAST(SPLIT_PART(t.display_id, '-', 2) AS BIGINT))
FROM tasks t
LEFT JOIN task_bundles tb ON t.task_bundle_id = tb.id
LEFT JOIN users u ON t.assigned_to = u.id
WHERE t.display_id IS NOT NULL
GROUP BY COALESCE(tb.role_id, u.role_id)
ON CONFLICT (role_id) DO UPDATE
SET last_sequence_number = GREATEST(role_task_counters.last_sequence_number, EXCLUDED.last_sequence_number);
