package com.lab.atlasmentor.enums;

import java.util.List;

/**
 * Single source of truth for the lead-import column list. Both {@code LeadTemplateService}
 * (generates the downloadable .xlsx template) and {@code LeadImportParseService} (parses
 * uploaded/linked files) read only from this enum for header names, required/optional,
 * and instructional text — adding or renaming a field is one change here, not several
 * files kept in sync by hand.
 *
 * This is the corrected field list matching the real lead-creation schema (the same one
 * {@code POST /api/students/onboarding} accepts) — see each constant's "maps to" javadoc.
 * There is no generic "Notes" column: it doesn't map to anything on {@code Student}, and
 * inventing a new home for it is a product decision, not something to guess at here.
 *
 * Declaration order is also template column order (see {@link #ordered()}).
 */
public enum LeadImportField {

    /** -> personalInfo.firstName */
    FIRST_NAME("First Name", true,
            List.of("first name", "firstname", "first"),
            "Required. The lead's given name."),

    /** -> personalInfo.lastName */
    LAST_NAME("Last Name", true,
            List.of("last name", "lastname", "last", "surname"),
            "Required. The lead's family name."),

    /** -> personalInfo.countryCode */
    COUNTRY_CODE("Country Code", true,
            List.of("country code", "phone country code", "isd code", "dial code"),
            "Required. The phone number's dialing/ISD code, e.g. +91."),

    /** -> personalInfo.phone */
    PHONE("Phone", true,
            List.of("phone", "mobile", "contact number", "phone number", "mobile number", "contact"),
            "Required. Phone number without the country code."),

    /** -> personalInfo.email */
    EMAIL("Email", false,
            List.of("email", "email address", "e-mail", "e mail"),
            "Optional."),

    /** -> destinationDetails.countryId, resolved by case-insensitive name lookup against the
     * existing Country table. An unmatched name leaves this field null on the created lead —
     * it does not fail the row (see LeadImportService). */
    DESTINATION_COUNTRY("Destination Country", false,
            List.of("destination country", "country", "target country", "study destination"),
            "Optional. Matched against the system's country list (case-insensitive); "
                    + "if not found, left unset rather than rejecting the row."),

    /** -> destinationDetails.universityId, resolved by case-insensitive name lookup against the
     * existing University table. An unmatched name leaves this field null on the created lead —
     * it does not fail the row (see LeadImportService). */
    TARGET_UNIVERSITY("Target University", false,
            List.of("target university", "university", "college", "target college"),
            "Optional. Matched against the system's university list (case-insensitive); "
                    + "if not found, left unset rather than rejecting the row."),

    /** -> destinationDetails.course (free text) */
    COURSE_NAME("Course Name", false,
            List.of("course name", "course", "program", "programme"),
            "Optional. Free text, stored exactly as provided."),

    /** -> destinationDetails.intake (free text) */
    INTAKE_PERIOD("Intake Period", false,
            List.of("intake period", "intake", "intake term"),
            "Optional. Free text, stored exactly as provided."),

    /** -> academicHistory.tenth.institution */
    TENTH_INSTITUTION("10th Institution", false,
            List.of("10th institution", "10th school", "tenth institution"),
            "Optional. Free text, stored exactly as provided."),

    /** -> academicHistory.tenth.year */
    TENTH_PASSING_YEAR("10th Passing Year", false,
            List.of("10th passing year", "10th year", "tenth passing year", "tenth year"),
            "Optional. A 4-digit year; left unset (not a row failure) if not a valid number."),

    /** -> academicHistory.tenth.score */
    TENTH_SCORE("10th Score/CGPA", false,
            List.of("10th score/cgpa", "10th score", "10th cgpa", "tenth score", "tenth score/cgpa"),
            "Optional. Free text — percentage or CGPA, stored exactly as provided."),

    /** -> academicHistory.twelfth.institution */
    TWELFTH_INSTITUTION("12th Institution", false,
            List.of("12th institution", "12th school", "twelfth institution"),
            "Optional. Free text, stored exactly as provided."),

    /** -> academicHistory.twelfth.year */
    TWELFTH_PASSING_YEAR("12th Passing Year", false,
            List.of("12th passing year", "12th year", "twelfth passing year", "twelfth year"),
            "Optional. A 4-digit year; left unset (not a row failure) if not a valid number."),

    /** -> academicHistory.twelfth.score */
    TWELFTH_SCORE("12th Score/CGPA", false,
            List.of("12th score/cgpa", "12th score", "12th cgpa", "twelfth score", "twelfth score/cgpa"),
            "Optional. Free text — percentage or CGPA, stored exactly as provided."),

    /** -> source (top-level on StudentOnboardingRequest, students.source) */
    SOURCE("Source", false,
            List.of("source", "lead source"),
            "Optional. Where the lead came from: Instagram, Youtube, AD, Website, a referring "
                    + "person's name, or any other free text. Stored exactly as provided — see LeadSource.");

    private final String header;
    private final boolean required;
    private final List<String> aliases;
    private final String instructions;

    LeadImportField(String header, boolean required, List<String> aliases, String instructions) {
        this.header = header;
        this.required = required;
        this.aliases = aliases;
        this.instructions = instructions;
    }

    public String getHeader() {
        return header;
    }

    public boolean isRequired() {
        return required;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getInstructions() {
        return instructions;
    }

    /**
     * Case-insensitive match of a raw spreadsheet/CSV header cell against this field's
     * canonical name or its alias list. Employees who don't use the generated template
     * exactly (Part 3's fallback) still get auto-mapped through this.
     */
    public boolean matchesHeader(String rawHeader) {
        if (rawHeader == null) {
            return false;
        }
        String normalized = rawHeader.trim().toLowerCase();
        if (normalized.equals(header.toLowerCase())) {
            return true;
        }
        return aliases.contains(normalized);
    }

    /** Enum declaration order = template column order. Named for readability at call sites. */
    public static LeadImportField[] ordered() {
        return values();
    }
}
