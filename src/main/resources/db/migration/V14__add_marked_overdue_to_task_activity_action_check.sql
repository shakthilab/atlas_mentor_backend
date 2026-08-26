-- ============================================================
-- V14__add_marked_overdue_to_task_activity_action_check.sql
--
-- Update task_activity.action CHECK constraint to include
-- MARKED_OVERDUE while preserving all existing valid actions.
-- ============================================================


-- ============================================================
-- 1. DROP EXISTING ACTION CHECK CONSTRAINT
-- ============================================================

ALTER TABLE public.task_activity
DROP CONSTRAINT IF EXISTS task_activity_action_check;


-- ============================================================
-- 2. ADD UPDATED ACTION CHECK CONSTRAINT
--
-- IMPORTANT:
-- Preserve ALL existing actions.
-- MARKED_OVERDUE is included as a valid action.
-- ============================================================

ALTER TABLE public.task_activity
    ADD CONSTRAINT task_activity_action_check
        CHECK (
    action IN (
    'CREATED',
    'STATUS_CHANGED',
    'PRIORITY_CHANGED',
    'ASSIGNED',
    'COMMENT_ADDED',
    'DUE_DATE_UPDATED',
    'DUE_TIME_UPDATED',
    'AUTO_GENERATED',
    'BUNDLE_EXECUTED',
    'BUNDLE_DEACTIVATED',
    'TASK_ESCALATED',
    'REMINDER_SENT',
    'MARKED_OVERDUE',
    'SUBTASK_ADDED',
    'ATTACHMENT_ADDED',
    'TAG_ADDED',
    'WATCHER_ADDED',
    'TIME_TRACKED',
    'LIST_CHANGED',
    'START_DATE_SET',
    'DEPENDENCY_ADDED'
    )
    );