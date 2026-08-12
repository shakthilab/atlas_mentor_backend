-- ============================================================
-- V16__sync_task_activity_action_check.sql
-- task_activity_action_check only allowed 8 of the 21 TaskAction enum values (V14 added just
-- MARKED_OVERDUE, one at a time, and fell behind again). AUTO_GENERATED - already used by
-- TaskGenerationService.createTaskActivity - was never in the list, so every such insert threw
-- a check-violation; TaskGenerationService.generateTasksForUser swallows it in a catch(Exception),
-- so it fails completely silently. Same trap for TemplateInstantiationService's newly-added
-- AUTO_GENERATED logging and TaskService's new DUE_TIME_UPDATED logging.
--
-- Widened to the full current enum (not just what's used today) so the next new TaskAction
-- doesn't require yet another migration just to stop silently failing.
-- ============================================================

ALTER TABLE task_activity DROP CONSTRAINT IF EXISTS task_activity_action_check;

ALTER TABLE task_activity ADD CONSTRAINT task_activity_action_check
    CHECK (action::text = ANY (ARRAY[
        'CREATED', 'STATUS_CHANGED', 'PRIORITY_CHANGED', 'ASSIGNED', 'COMMENT_ADDED',
        'DUE_DATE_UPDATED', 'DUE_TIME_UPDATED', 'AUTO_GENERATED', 'BUNDLE_EXECUTED',
        'BUNDLE_DEACTIVATED', 'TASK_ESCALATED', 'REMINDER_SENT', 'MARKED_OVERDUE',
        'SUBTASK_ADDED', 'ATTACHMENT_ADDED', 'TAG_ADDED', 'WATCHER_ADDED', 'TIME_TRACKED',
        'LIST_CHANGED', 'START_DATE_SET', 'DEPENDENCY_ADDED'
    ]::text[]));
