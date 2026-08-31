-- ============================================================
-- V34__allow_hard_delete_of_templates_with_tasks.sql
-- Allow hard-deleting a role template (task_bundles row) even when real
-- tasks were already generated from it.
--
-- tasks.task_bundle_id -> task_bundles(id) previously had no explicit
-- ON DELETE behavior (defaulting to NO ACTION), so RoleTemplateService
-- #hardDeleteTemplate blocked with a 409 data-conflict whenever any task
-- had ever been instantiated from the template - see that method's
-- javadoc for the original reasoning (avoid silently orphaning task
-- history). Product decision: a hard delete should be allowed to proceed
-- regardless - the tasks themselves are kept, only their link back to the
-- now-deleted template is cleared.
-- ============================================================

DO $$
DECLARE
    con RECORD;
BEGIN
    FOR con IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'tasks'::regclass
          AND confrelid = 'task_bundles'::regclass
          AND contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE tasks DROP CONSTRAINT %I', con.conname);
    END LOOP;

    ALTER TABLE tasks
        ADD CONSTRAINT fk_tasks_task_bundle
        FOREIGN KEY (task_bundle_id) REFERENCES task_bundles(id) ON DELETE SET NULL;
END $$;
