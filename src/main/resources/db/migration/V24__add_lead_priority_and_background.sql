-- ============================================================
-- V24__add_lead_priority_and_background.sql
-- Add lead priority classification (P1/P2/P3 tier + tier-specific subcategory) and
-- background (Educated/Less Educated) to students, per the priority framework doc.
--
-- All three columns are nullable: a lead is not required to be classified at creation
-- (confirmed with the team - see LeadPriority/LeadPrioritySubCategory/LeadBackground, the
-- single shared source of truth for the allowed values, used by manual create/edit
-- validation, the lead-import template, and both import parsers). priority_sub_category is
-- stored independently of priority (also nullable even when priority is set) - the tier/
-- subcategory pairing is enforced at the application layer, not via a DB constraint, since
-- Postgres CHECK constraints can't easily express "this enum value must be one of the
-- subset belonging to that other column's value" without duplicating the mapping in SQL.
-- ============================================================

ALTER TABLE students ADD COLUMN IF NOT EXISTS priority VARCHAR(10);
ALTER TABLE students ADD COLUMN IF NOT EXISTS priority_sub_category VARCHAR(50);
ALTER TABLE students ADD COLUMN IF NOT EXISTS background VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_students_priority ON students (priority);
