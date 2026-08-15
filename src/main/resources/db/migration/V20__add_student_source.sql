-- New "Source" field on leads: where the lead came from (Person/Instagram/Youtube/AD/Website/Other).
-- Free text, not a DB enum/CHECK constraint - the Person and Other options in LeadSource are
-- free-typed by the user, so this column just stores whatever text is resolved on the way in.
ALTER TABLE students ADD COLUMN source VARCHAR(255);
