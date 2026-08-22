-- Three FK constraints have never once been successfully created in this database:
-- day_approvals.approver_user_id -> users, day_workspaces.employee_user_id -> users,
-- and template_days.role_template_id -> task_bundles. Hibernate (ddl-auto=update)
-- retries adding all three on every single startup and fails every single time
-- (logged as a WARN, not fatal, which is why it's gone unnoticed):
--   ERROR: insert or update on table "day_approvals" violates foreign key constraint
--   "fk3s2oi8k5ogoko8ec2qcat6ylb" - Detail: Key (approver_user_id)=(2) is not present
--   in table "users".
--   ERROR: ... "day_workspaces" ... "fkj4wm9v4ig1987v3d5yb9p5lef" ...
--   Key (employee_user_id)=(9) is not present in table "users".
--   ERROR: ... "template_days" ... "fk7v3rcuuesg7ddlc0kkw44dcmq" ...
--   Key (role_template_id)=(1) is not present in table "task_bundles".
-- Root cause: orphaned rows left behind by an earlier reset of users/task_bundles that
-- moved the id sequences on without cleaning up rows still pointing at the old ids.
-- Until this data is cleaned up, none of these three FKs are actually enforced.
--
-- Cleaned up bottom-up (children before parents) per each orphan's actual dependents:
--   day_workspaces(employee_user_id not in users) has children in tasks
--     (day_workspace_id nullable - detach, don't delete real task data) and in
--     day_approvals (day_workspace_id NOT NULL - must delete).
--   template_days(role_template_id not in task_bundles) has children in template_tasks
--     (template_day_id NOT NULL - must delete).
-- day_approvals rows with a bad approver_user_id (independent of day_workspace_id) are
-- cleaned up in the same DELETE.

-- 1. Detach real tasks from the orphaned day_workspace instead of deleting task data.
UPDATE tasks
SET day_workspace_id = NULL
WHERE day_workspace_id IN (
    SELECT id FROM day_workspaces WHERE employee_user_id NOT IN (SELECT id FROM users)
);

-- 2. day_approvals rows that are dead either via their workspace or their approver.
DELETE FROM day_approvals
WHERE day_workspace_id IN (
    SELECT id FROM day_workspaces WHERE employee_user_id NOT IN (SELECT id FROM users)
)
OR approver_user_id NOT IN (SELECT id FROM users);

-- 3. The orphaned day_workspaces rows themselves.
DELETE FROM day_workspaces
WHERE employee_user_id NOT IN (SELECT id FROM users);

-- 4. template_tasks under an orphaned template_days row (NOT NULL FK - can't detach).
DELETE FROM template_tasks
WHERE template_day_id IN (
    SELECT id FROM template_days WHERE role_template_id NOT IN (SELECT id FROM task_bundles)
);

-- 5. The orphaned template_days rows themselves.
DELETE FROM template_days
WHERE role_template_id NOT IN (SELECT id FROM task_bundles);

-- 6. Now that the blocking data is gone, actually create the 3 FK constraints that
--    Hibernate has been silently failing to add on every boot.
ALTER TABLE day_approvals
    DROP CONSTRAINT IF EXISTS fk3s2oi8k5ogoko8ec2qcat6ylb;
ALTER TABLE day_approvals
    ADD CONSTRAINT fk3s2oi8k5ogoko8ec2qcat6ylb
    FOREIGN KEY (approver_user_id) REFERENCES users;

ALTER TABLE day_workspaces
    DROP CONSTRAINT IF EXISTS fkj4wm9v4ig1987v3d5yb9p5lef;
ALTER TABLE day_workspaces
    ADD CONSTRAINT fkj4wm9v4ig1987v3d5yb9p5lef
    FOREIGN KEY (employee_user_id) REFERENCES users;

ALTER TABLE template_days
    DROP CONSTRAINT IF EXISTS fk7v3rcuuesg7ddlc0kkw44dcmq;
ALTER TABLE template_days
    ADD CONSTRAINT fk7v3rcuuesg7ddlc0kkw44dcmq
    FOREIGN KEY (role_template_id) REFERENCES task_bundles;
