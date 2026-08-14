package com.lab.atlasmentor.dto;

/**
 * One row of {@code GET /api/leads/import/fields} — the column name + required flag exactly as
 * {@link com.lab.atlasmentor.enums.LeadImportField} defines it, so the frontend's Import Data
 * modal never hand-maintains a second copy of this list.
 */
public record LeadImportFieldDto(String column, boolean required) {
}
