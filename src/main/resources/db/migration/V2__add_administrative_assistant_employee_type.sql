ALTER TABLE employee_details DROP CONSTRAINT IF EXISTS employee_details_employee_type_check;

ALTER TABLE employee_details
    ADD CONSTRAINT employee_details_employee_type_check
    CHECK (employee_type IN (
        'ADMIN',
        'MANAGER',
        'ADMINISTRATIVE_ASSISTANT',
        'SENIOR_COUNSELLOR',
        'JUNIOR_COUNSELLOR',
        'VIDEO_EDITOR',
        'GRAPHIC_DESIGNER',
        'WEB_DEV'
    ));