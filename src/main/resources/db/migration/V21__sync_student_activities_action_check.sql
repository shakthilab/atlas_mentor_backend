-- ============================================================
-- V21__sync_student_activities_action_check.sql
-- Both student_activities_action_check and students_status_check only allowed 5 of the 8
-- current StudentStatus enum values (LEAD, REGISTERED, LOST, STUDENT, PROSPECTIVE) and even
-- included 'STUDENT', which was never a StudentStatus value. CONTACTED, IN_PROGRESS, FOLLOWUP,
-- and DOCUMENT_SUBMITTED were added to the enum later but neither constraint was updated, so
-- StudentService#updateStudentStatus moving a student to any of those four statuses throws a
-- check-violation (first logging to student_activities, then - once that's fixed - updating
-- students.status itself) and the whole status update rolls back with a 409.
--
-- Same drift the task_activity_action_check had (see V16) - widened both to the full current
-- StudentStatus enum so the next new status doesn't require yet another migration.
-- ============================================================

ALTER TABLE student_activities DROP CONSTRAINT IF EXISTS student_activities_action_check;

ALTER TABLE student_activities ADD CONSTRAINT student_activities_action_check
    CHECK (action::text = ANY (ARRAY[
        'LEAD', 'REGISTERED', 'LOST', 'PROSPECTIVE', 'CONTACTED', 'IN_PROGRESS',
        'FOLLOWUP', 'DOCUMENT_SUBMITTED'
    ]::text[]));

ALTER TABLE students DROP CONSTRAINT IF EXISTS students_status_check;

ALTER TABLE students ADD CONSTRAINT students_status_check
    CHECK (status::text = ANY (ARRAY[
        'LEAD', 'REGISTERED', 'LOST', 'PROSPECTIVE', 'CONTACTED', 'IN_PROGRESS',
        'FOLLOWUP', 'DOCUMENT_SUBMITTED'
    ]::text[]));
