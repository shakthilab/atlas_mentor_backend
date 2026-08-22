-- TemplateInstantiationService (and OverdueTaskSchedulerService / TaskGenerationService)
-- hardcode SYSTEM_USER_ID / ADMIN_USER_ID = 1L as the actor for auto-generated tasks
-- (assigned_by / created_by / updated_by). On a database where the users identity
-- sequence has moved past 1 - as this one has: the real admin is id 29, not 1 - every
-- template-driven task insert fails:
--   ERROR: insert or update on table "tasks" violates foreign key constraint
--   "fkj4ejbrghwmfxncp8ok1vc6129" - Detail: Key (assigned_by)=(1) is not present in
--   table "users".
-- and gets misreported as a harmless "duplicate instantiation" by the same
-- DataIntegrityViolationException catch in TemplateInstantiationService#instantiateDayFor
-- that V26 worked around for the source_type check constraint, leaving tasksCreated=0.
--
-- This is a stopgap, not a real fix: it patches THIS database to match what the code
-- assumes, rather than making the code stop assuming a magic ID. A fresh database is
-- unaffected either way (its first-ever user naturally lands on id 1 via
-- AdminInitializer). See TemplateInstantiationService.SYSTEM_USER_ID /
-- OverdueTaskSchedulerService.SYSTEM_USER_ID / TaskGenerationService.ADMIN_USER_ID for
-- the actual assumption that should eventually be replaced with a real lookup.
--
-- No-ops (via the FROM roles subquery returning zero rows) if no ADMIN role exists yet,
-- which is the case on a genuinely fresh database at migration time - AdminInitializer
-- seeds ADMIN as a post-startup CommandLineRunner, after Flyway has already run.
INSERT INTO users (id, first_name, last_name, email, phone, password, is_verified,
                    role_id, status, failed_login_attempts, created_at, updated_at)
SELECT 1, 'System', 'Automation',
       'system.automation@atlasmentor.internal', '0000000001',
       -- Syntactically valid bcrypt hash with no known matching plaintext - this
       -- account is a marker row for scheduled/automated actions, never meant to
       -- authenticate.
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       true, r.id, 'ACTIVE', 0, now(), now()
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT (id) DO NOTHING;
