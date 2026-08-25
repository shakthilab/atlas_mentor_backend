-- ============================================================
-- V31__add_comment_edit_support.sql
-- Adds edit support for task comments (Daily Workspace comment popup):
--
-- 1. task_comments.edited: explicit flag set true only when a comment is
--    actually updated via TaskService#updateComment, so the UI can show an
--    "(edited)" marker. Not derived by comparing created_at/updated_at,
--    since BaseEntity's @PrePersist sets both independently on insert and
--    they can differ by a few microseconds even for a comment that was
--    never edited.
--
-- 2. task_activity_action_check: widened to allow the new COMMENT_EDITED
--    TaskAction (see V16 - this constraint has to be kept in sync with the
--    TaskAction enum by hand, or activity logging for the new action fails
--    the check and, depending on the caller, can fail silently).
-- ============================================================

ALTER TABLE task_comments ADD COLUMN IF NOT EXISTS edited BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE task_activity DROP CONSTRAINT IF EXISTS task_activity_action_check;

ALTER TABLE task_activity ADD CONSTRAINT task_activity_action_check
    CHECK (action::text = ANY (ARRAY[
        'CREATED', 'STATUS_CHANGED', 'PRIORITY_CHANGED', 'ASSIGNED', 'COMMENT_ADDED',
        'COMMENT_EDITED', 'DUE_DATE_UPDATED', 'DUE_TIME_UPDATED', 'AUTO_GENERATED',
        'BUNDLE_EXECUTED', 'BUNDLE_DEACTIVATED', 'TASK_ESCALATED', 'REMINDER_SENT',
        'MARKED_OVERDUE', 'SUBTASK_ADDED', 'ATTACHMENT_ADDED', 'TAG_ADDED', 'WATCHER_ADDED',
        'TIME_TRACKED', 'LIST_CHANGED', 'START_DATE_SET', 'DEPENDENCY_ADDED'
    ]::text[]));
