-- ============================================================
-- V37__allow_voice_only_task_comments.sql
-- Allow task_comments.comment to be NULL.
--
-- A comment can now be a voice note with no typed text: the client
-- creates the task_comments row first (POST /api/tasks/{id}/comments,
-- comment omitted/null) to get a commentId, then attaches the recording
-- via POST /api/tasks/{id}/attachments/upload with that commentId (see
-- TaskAttachmentUploadService, which already accepts audio/webm). The
-- column was NOT NULL, so the first call always failed with
-- "Comment is required" before a voice-only comment could exist.
-- ============================================================

ALTER TABLE task_comments ALTER COLUMN comment DROP NOT NULL;
