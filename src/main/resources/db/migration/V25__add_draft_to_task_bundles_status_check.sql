-- BundleStatus enum gained a DRAFT value (com.lab.atlasmentor.enums.BundleStatus) for
-- role template drafts (see RoleTemplateService), but the DB check constraint on
-- task_bundles.status was never updated, so creating a template fails with:
--   ERROR: new row for relation "task_bundles" violates check constraint "task_bundles_status_check"
ALTER TABLE task_bundles DROP CONSTRAINT IF EXISTS task_bundles_status_check;

ALTER TABLE task_bundles
    ADD CONSTRAINT task_bundles_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DRAFT'));
