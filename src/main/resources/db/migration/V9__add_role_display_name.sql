-- ============================================================
-- 1. ADD display_name COLUMN TO roles TABLE
-- ============================================================
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);

-- ============================================================
-- 2. POPULATE display names for known roles
-- ============================================================
UPDATE roles SET display_name = 'Admin' WHERE name = 'ADMIN';
UPDATE roles SET display_name = 'Manager' WHERE name = 'MANAGER';
UPDATE roles SET display_name = 'Administrative Assistant' WHERE name = 'ADMINISTRATIVE_ASSISTANT';
UPDATE roles SET display_name = 'Branch Partner' WHERE name = 'BRANCH_PARTNER';
UPDATE roles SET display_name = 'Video Editor' WHERE name = 'VIDEO_EDITOR';
UPDATE roles SET display_name = 'Senior Counsellor' WHERE name = 'SENIOR_COUNSELLOR';
UPDATE roles SET display_name = 'Junior Counsellor' WHERE name = 'JUNIOR_COUNSELLOR';
UPDATE roles SET display_name = 'Counsellor' WHERE name = 'COUNSELLOR';
UPDATE roles SET display_name = 'Referral' WHERE name = 'REFERRAL';
UPDATE roles SET display_name = 'Company' WHERE name = 'COMPANY';
UPDATE roles SET display_name = 'Student' WHERE name = 'STUDENT';

-- ============================================================
-- 3. FALLBACK for any other/future role rows: derive a Title Case
--    display name from the underscore-separated code name so no
--    row is left with a NULL display_name.d
-- ============================================================
UPDATE roles
SET display_name = (
    SELECT string_agg(initcap(lower(word)), ' ')
    FROM unnest(string_to_array(name, '_')) AS word
)
WHERE display_name IS NULL;
