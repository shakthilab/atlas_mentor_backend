-- V5__migrate_referral_resources_to_multi_owner.sql
--
-- Referral resources already use the many-to-many ownership model
-- through referral_resource_owners.
--
-- The join table and existing owner mappings are already present,
-- so no data migration is required here.

-- Ensure the join table exists for fresh databases.
CREATE TABLE IF NOT EXISTS referral_resource_owners (
                                                        resource_id BIGINT NOT NULL,
                                                        owner_id    BIGINT NOT NULL,

                                                        CONSTRAINT referral_resource_owners_pkey
                                                        PRIMARY KEY (resource_id, owner_id),

    CONSTRAINT fk_rro_resource
    FOREIGN KEY (resource_id)
    REFERENCES referral_resources(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_rro_owner
    FOREIGN KEY (owner_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );