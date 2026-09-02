-- ============================================================
-- V35__add_proof_required.sql
-- Add proof_required to template_tasks and tasks: a per-task yes/no
-- toggle (set by an admin in the Role Template day drawer) for whether
-- the employee must attach at least one proof-section file (photo/
-- document/voice note) before the task can be marked DONE.
--
-- tasks.proof_required is a SNAPSHOT copied from template_tasks at the
-- moment of instantiation (TemplateInstantiationService), same pattern
-- already established for title/description/priority - editing a
-- template's proof_required afterwards never retroactively changes
-- tasks already generated from it.
-- ============================================================

ALTER TABLE template_tasks ADD COLUMN IF NOT EXISTS proof_required BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS proof_required BOOLEAN NOT NULL DEFAULT false;