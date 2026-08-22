-- tasks.source_type has a CHECK constraint that was never captured in a migration
-- (it exists in the live DB but nowhere in db/migration), and it does not allow
-- TaskSource.TEMPLATE_GENERATED (com.lab.atlasmentor.enums.TaskSource), so every
-- template-driven task instantiation (TemplateInstantiationService, run by the
-- nightly cron / startup catch-up / periodic safety-net / Publish) fails with:
--   ERROR: new row for relation "tasks" violates check constraint "tasks_source_type_check"
-- and gets misreported as a harmless "duplicate instantiation" by the broad
-- DataIntegrityViolationException catch in TemplateInstantiationService#instantiateDayFor,
-- masking the real failure and leaving tasksCreated=0 for every employee, every day.
--
-- TaskSource.TASK_BUNDLE (still set by TaskGenerationService) has the same problem
-- and is documented as @Deprecated in TaskSource.java for exactly this reason.
--
-- Widen the constraint to allow every value the TaskSource enum defines.
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_source_type_check;

ALTER TABLE tasks
    ADD CONSTRAINT tasks_source_type_check
    CHECK (source_type IN ('MANUAL', 'TASK_BUNDLE', 'TEMPLATE_GENERATED', 'SYSTEM_AUTOMATION', 'WORKFLOW'));
