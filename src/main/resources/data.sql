-- Data migration script for role table refactoring
-- This script should be executed after the application starts with the new schema

-- Insert default roles if they don't exist
INSERT IGNORE INTO roles (name, description) VALUES 
('ADMIN', 'System administrator with full access'),
('MANAGER', 'Branch manager with limited administrative access'),
('EMPLOYEE', 'Regular employee with basic access'),
('REFERRAL', 'Referral partner with limited access'),
('COMPANY', 'Company account with business access'),
('STUDENT', 'Student account with learning access');

-- Migrate existing users from role column to user_roles table
-- This assumes the old role column still exists during migration
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT 
    u.id as user_id,
    r.id as role_id,
    NOW() as created_at,
    NOW() as updated_at
FROM users u
JOIN roles r ON r.name = u.role
WHERE u.role IS NOT NULL;

-- After successful migration, the role column can be dropped
-- ALTER TABLE users DROP COLUMN role;
