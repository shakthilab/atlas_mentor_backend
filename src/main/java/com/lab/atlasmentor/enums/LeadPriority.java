package com.lab.atlasmentor.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The P1/P2/P3 lead-priority tiers, from the priority framework doc. Each tier owns a fixed,
 * disjoint subcategory list — see {@link LeadPrioritySubCategory} — and together these two
 * enums are the single shared source of truth for lead priority classification: used by manual
 * create/edit validation ({@code StudentService}), the lead-import template generator
 * ({@code LeadTemplateService}), and both import parsers ({@code LeadImportService} /
 * {@code LeadImportParseService}). Do not hand-maintain the tier or subcategory lists anywhere
 * else — add/rename a tier or subcategory here and it shows up everywhere automatically.
 */
public enum LeadPriority {
    P1("High"),
    P2("Medium"),
    P3("Low");

    private final String displayLabel;

    LeadPriority(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    /** This tier's fixed subcategory list, in declaration order. */
    public List<LeadPrioritySubCategory> subCategories() {
        return Arrays.stream(LeadPrioritySubCategory.values())
                .filter(sub -> sub.getTier() == this)
                .collect(Collectors.toList());
    }

    /**
     * Case-insensitive match against the enum name (P1/P2/P3) or its display label
     * (High/Medium/Low), consistent with the alias-matching approach {@link LeadImportField}
     * uses for column headers. Returns {@code null} rather than throwing — callers decide
     * how to report an unrecognized value in their own context (manual API vs. import row).
     */
    public static LeadPriority fromText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        for (LeadPriority priority : values()) {
            if (priority.name().equalsIgnoreCase(normalized) || priority.displayLabel.equalsIgnoreCase(normalized)) {
                return priority;
            }
        }
        return null;
    }
}
