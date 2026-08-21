package com.lab.atlasmentor.enums;

/**
 * Fixed subcategory list per {@link LeadPriority} tier, from the priority framework doc. Each
 * constant belongs to exactly one tier; a subcategory submitted against a different tier than
 * the one it's declared under here (e.g. Priority=P1 with prioritySubCategory=WARM_LEADS, which
 * belongs to P2) must be rejected — see {@link #requireBelongsToTier}, used by both manual
 * create/edit validation and import row validation so the "wrong tier" rule can't drift between
 * the two call sites.
 */
public enum LeadPrioritySubCategory {

    // ── P1 — High ───────────────────────────────────────────────────────────
    HOT_LEADS(LeadPriority.P1, "Hot Leads"),
    DIRECT_REFERRAL_LEADS(LeadPriority.P1, "Direct Referral Leads"),
    HIGH_BUDGET_PREMIUM_LEADS(LeadPriority.P1, "High Budget / Premium Leads"),
    PARENT_READY_LEADS(LeadPriority.P1, "Parent Ready Leads"),
    IMMEDIATE_INTAKE(LeadPriority.P1, "Immediate Intake"),
    CLEAR_DECISION_MAKERS(LeadPriority.P1, "Clear Decision Makers"),

    // ── P2 — Medium ─────────────────────────────────────────────────────────
    WARM_LEADS(LeadPriority.P2, "Warm Leads"),
    CONFUSED_LEADS(LeadPriority.P2, "Confused Leads"),
    OVERTHINKERS_QUESTION_MACHINE(LeadPriority.P2, "Overthinkers / Question Machine"),
    MEDIUM_BUDGET_LEADS(LeadPriority.P2, "Medium Budget Leads"),
    COMPARING_LEADS(LeadPriority.P2, "Comparing Leads"),
    STUDENT_DRIVEN(LeadPriority.P2, "Student Driven"),
    FLEXIBLE_BUT_UNSURE(LeadPriority.P2, "Flexible But Unsure"),

    // ── P3 — Low ────────────────────────────────────────────────────────────
    COLD_LEADS(LeadPriority.P3, "Cold Leads"),
    ENQUIRY_INFO_SEEKERS(LeadPriority.P3, "Enquiry / Info Seekers"),
    VERY_LOW_BUDGET_UNREALISTIC(LeadPriority.P3, "Very Low Budget / Unrealistic"),
    NEXT_YEAR_NO_URGENCY(LeadPriority.P3, "Next Year / No Urgency"),
    NO_DECISION_AUTHORITY(LeadPriority.P3, "No Decision Authority"),
    HIGHLY_CONFUSED_NO_DIRECTION(LeadPriority.P3, "Highly Confused / No Direction"),
    FAKE_CASUAL_LEADS(LeadPriority.P3, "Fake / Casual Leads");

    private final LeadPriority tier;
    private final String label;

    LeadPrioritySubCategory(LeadPriority tier, String label) {
        this.tier = tier;
        this.label = label;
    }

    public LeadPriority getTier() {
        return tier;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Case-insensitive match against the enum name or its display label, ignoring which tier
     * it belongs to. Used to resolve free text (import cells, or an already-invalid API value)
     * into a subcategory before checking it against the tier that was actually submitted — see
     * {@link #requireBelongsToTier} — so a wrong-tier submission can report which tier the
     * subcategory *does* belong to, instead of a bare "invalid value".
     */
    public static LeadPrioritySubCategory fromTextAnyTier(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        for (LeadPrioritySubCategory sub : values()) {
            if (sub.name().equalsIgnoreCase(normalized) || sub.label.equalsIgnoreCase(normalized)) {
                return sub;
            }
        }
        return null;
    }

    /**
     * The single shared check: does this subcategory belong to the given tier? Throws
     * {@link IllegalArgumentException} with a clear, human-readable message on any mismatch —
     * both a subcategory with no tier at all, and a subcategory belonging to a different tier.
     * Callers wrap the message in whatever exception type fits their context (a
     * {@code BusinessException} for the manual API, a row-specific exception for imports); the
     * message text itself is defined once, here.
     */
    public static void requireBelongsToTier(LeadPrioritySubCategory subCategory, LeadPriority tier) {
        if (subCategory == null) {
            return;
        }
        if (tier == null) {
            throw new IllegalArgumentException("prioritySubCategory '" + subCategory.label
                    + "' was provided without a priority tier — set priority (P1/P2/P3) first.");
        }
        if (subCategory.tier != tier) {
            throw new IllegalArgumentException("'" + subCategory.label + "' is not a valid subcategory for priority "
                    + tier.name() + " (" + tier.getDisplayLabel() + ") — it belongs to " + subCategory.tier.name()
                    + " (" + subCategory.tier.getDisplayLabel() + ").");
        }
    }
}
