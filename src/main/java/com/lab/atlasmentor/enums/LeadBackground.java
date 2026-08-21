package com.lab.atlasmentor.enums;

/**
 * The lead's educational background — a simple two-option dropdown, part of the same
 * priority-framework classification as {@link LeadPriority}/{@link LeadPrioritySubCategory}.
 */
public enum LeadBackground {
    EDUCATED("Educated"),
    LESS_EDUCATED("Less Educated");

    private final String displayLabel;

    LeadBackground(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    /** Case-insensitive match against the enum name or its display label. */
    public static LeadBackground fromText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        for (LeadBackground background : values()) {
            if (background.name().equalsIgnoreCase(normalized) || background.displayLabel.equalsIgnoreCase(normalized)) {
                return background;
            }
        }
        return null;
    }
}
