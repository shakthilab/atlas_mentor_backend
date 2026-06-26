-- Migrate referral_resources from single owner_id column to many-to-many join table

-- 1. Create the join table
CREATE TABLE IF NOT EXISTS referral_resource_owners (
    resource_id BIGINT NOT NULL,
    owner_id    BIGINT NOT NULL,
    CONSTRAINT referral_resource_owners_pkey PRIMARY KEY (resource_id, owner_id),
    CONSTRAINT fk_rro_resource FOREIGN KEY (resource_id) REFERENCES referral_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_rro_owner    FOREIGN KEY (owner_id)    REFERENCES users(id)              ON DELETE CASCADE
);

-- 2. Carry existing single-owner rows into the join table
INSERT INTO referral_resource_owners (resource_id, owner_id)
SELECT id, owner_id FROM referral_resources;

-- 3. Drop FK and column that are no longer needed
ALTER TABLE referral_resources DROP CONSTRAINT IF EXISTS fk_referral_resources_owner;
ALTER TABLE referral_resources DROP COLUMN IF EXISTS owner_id;
