-- Role Template days had no way to scope a task to a single calendar month: TemplateDay
-- only ever carried day_number (1-31), by design, so the daily instantiation cron could
-- match "today.getDayOfMonth()" against the same recurring day every month (see commit
-- 84815aa). The admin UI's month dropdown implied per-month independence that the schema
-- never actually backed, so a task added on "Day 22" while viewing August also showed up
-- on Day 22 in September - there was only ever one row for day_number 22 per template.
--
-- Adding nullable month/year: NULL/NULL keeps today's recurring behavior unchanged for
-- every existing row. A row with both set applies only to that exact month, letting the
-- admin UI (and TemplateInstantiationService) tell a specific month's "Day 22" apart from
-- every other month's.
ALTER TABLE template_days ADD COLUMN IF NOT EXISTS month INTEGER;
ALTER TABLE template_days ADD COLUMN IF NOT EXISTS year INTEGER;

-- template_days already carries a plain UNIQUE (role_template_id, day_number) constraint
-- (uk_template_days_bundle_day) that predates this migration - absent from every migration
-- file and from the current TemplateDay entity, so it was evidently added by hand or by an
-- earlier since-reverted @UniqueConstraint annotation, and ddl-auto=update never drops it on
-- its own. It has to go: it's the actual thing enforcing "only one Day 22 ever, no matter
-- the month" at the DB level, so a scoped and a recurring (or two scoped) row for the same
-- day_number would fail on INSERT. Replaced with two partial indexes so a plain unique
-- index doesn't treat every NULL/NULL (recurring) pair as accidentally distinct:
--   - at most one recurring (month/year NULL) row per (template, day_number)
--   - at most one scoped row per (template, day_number, month, year)
ALTER TABLE template_days DROP CONSTRAINT IF EXISTS uk_template_days_bundle_day;

CREATE UNIQUE INDEX IF NOT EXISTS uk_template_days_recurring
    ON template_days (role_template_id, day_number)
    WHERE month IS NULL AND year IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_template_days_scoped
    ON template_days (role_template_id, day_number, month, year)
    WHERE month IS NOT NULL AND year IS NOT NULL;