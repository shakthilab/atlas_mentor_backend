-- ============================================================
-- V33__fix_tasks_source_template_task_fk_on_delete.sql
-- Restore V10's intended ON DELETE SET NULL behavior for
-- tasks.source_template_task_id -> template_tasks(id).
--
-- On an environment whose schema predates Flyway (built by Hibernate's
-- ddl-auto and later baselined - see application-dev.properties'
-- baseline-version comment), Hibernate had already auto-generated its own
-- FK for this column before V10 ran, with a hashed name and the default
-- NO ACTION instead of SET NULL. Since that hashed constraint already
-- satisfied "a FK exists here", V10's own ADD CONSTRAINT (a different,
-- explicitly-named constraint) just became a second, redundant FK -
-- Postgres allows more than one FK on the same column - leaving the
-- stricter NO ACTION one to actually enforce deletes.
--
-- Effect of the bug: deleting a TemplateTask that already generated real
-- Task rows (RoleTemplateService#deleteTaskFromDay) fails with a raw
-- DataIntegrityViolationException / 409 instead of the intended behavior -
-- detach those tasks from their template origin (source_template_task_id
-- -> NULL) and let the delete proceed, since "template changes never
-- cascade" to already-instantiated tasks (see RoleTemplateService's
-- publish/reactivate comments for the same principle elsewhere).
-- ============================================================

DO $$
DECLARE
    con RECORD;
BEGIN
    FOR con IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'tasks'::regclass
          AND confrelid = 'template_tasks'::regclass
          AND contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE tasks DROP CONSTRAINT %I', con.conname);
    END LOOP;

    ALTER TABLE tasks
        ADD CONSTRAINT fk_tasks_source_template_task
        FOREIGN KEY (source_template_task_id) REFERENCES template_tasks(id) ON DELETE SET NULL;
END $$;
