ALTER TABLE referral_resources
    DROP CONSTRAINT IF EXISTS referral_resources_owner_type_check,
    DROP CONSTRAINT IF EXISTS chk_owner_type;

ALTER TABLE referral_resources
    ADD CONSTRAINT referral_resources_owner_type_check
    CHECK (owner_type IN (
        'REFERRAL',
        'COMPANY',
        'SENIOR_COUNSELLOR',
        'JUNIOR_COUNSELLOR',
        'VIDEO_EDITOR',
        'COUNSELLOR',
        'MANAGER',
        'BRANCH_PARTNER',
        'ADMINISTRATIVE_ASSISTANT'
    ));
