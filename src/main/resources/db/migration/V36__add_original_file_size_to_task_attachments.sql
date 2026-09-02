-- ============================================================
-- V36__add_original_file_size_to_task_attachments.sql
-- Add original_file_size to task_attachments: the file's size before
-- server-side compression/normalization (see TaskAttachmentUploadService
-- / MediaCompressionService). file_size already on this table continues
-- to mean "size actually stored" (post-compression) - having both lets
-- us confirm compression is actually helping without guessing, per the
-- multi-type upload spec.
--
-- Nullable: attachments registered via the older
-- POST /api/tasks/{id}/attachments (AddAttachmentRequest, which just
-- records a URL the client already uploaded elsewhere - no file ever
-- passes through this server) have no original size to record.
-- ============================================================

ALTER TABLE task_attachments ADD COLUMN IF NOT EXISTS original_file_size BIGINT;