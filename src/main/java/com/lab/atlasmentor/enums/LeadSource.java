package com.lab.atlasmentor.enums;

/**
 * Fixed options for the "Source" field shown when adding a lead — where the lead came from.
 *
 * PERSON and OTHER are free-text entries: the UI collects a typed value for them (a referring
 * person's name for PERSON, anything else for OTHER), and that raw text — not the enum constant
 * name — is what gets stored in {@code students.source}. The remaining four are fixed channels.
 * This is why {@code students.source} is a plain VARCHAR column rather than an {@code @Enumerated}
 * one: two of the six options are inherently free text, so this enum drives the dropdown/validation
 * on the way in, it doesn't constrain the column itself.
 */
public enum LeadSource {
    PERSON("Person"),
    INSTAGRAM("Instagram"),
    YOUTUBE("Youtube"),
    AD("AD"),
    WEBSITE("Website"),
    OTHER("Other");

    private final String displayName;

    LeadSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
