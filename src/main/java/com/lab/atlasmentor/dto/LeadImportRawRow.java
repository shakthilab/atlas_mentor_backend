package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.LeadImportField;

import java.util.Map;

/**
 * One data row from an uploaded/linked lead file, already column-mapped against
 * {@link LeadImportField} by the parser (Excel or CSV) — the rest of the import
 * pipeline (LeadImportService) never sees raw column indices/header strings again.
 *
 * @param rowNumber 1-indexed as the employee would see it in Excel/Sheets (header = row 1).
 */
public record LeadImportRawRow(int rowNumber, Map<LeadImportField, String> values) {
}
