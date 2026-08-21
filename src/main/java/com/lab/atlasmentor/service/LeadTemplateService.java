package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.LeadBackground;
import com.lab.atlasmentor.enums.LeadImportField;
import com.lab.atlasmentor.enums.LeadPriority;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Generates the canonical lead-import .xlsx template (Part 1) server-side, from
 * {@link LeadImportField} — the exact same field list {@code LeadImportParseService}
 * validates uploads against. Nothing here is hand-maintained; add a field to the enum
 * and it shows up in both the "Leads" sheet and the "Instructions" sheet automatically.
 */
@Service
public class LeadTemplateService {

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            LeadImportField[] fields = LeadImportField.ordered();

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            buildLeadsSheet(workbook, fields, headerStyle);
            buildInstructionsSheet(workbook, fields, headerStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate lead import template", e);
        }
    }

    private void buildLeadsSheet(Workbook workbook, LeadImportField[] fields, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Leads");

        // Header row only — no demo/example data row. Column-by-column guidance lives on the
        // Instructions sheet instead.
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < fields.length; i++) {
            Cell cell = headerRow.createCell(i);
            // " *" marks required columns for the employee filling this in; the parser
            // strips it back off before matching (see LeadImportParseService#stripRequiredMarker).
            cell.setCellValue(fields[i].getHeader() + (fields[i].isRequired() ? " *" : ""));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 26 * 256);
        }
    }

    /**
     * A second sheet spelling out, per column: required or optional, what it maps to, and any
     * special handling (the Destination Country / Target University name-to-ID lookup behavior
     * in particular — those two are matched against the system's existing lists and silently
     * left blank on the lead if no match is found, rather than failing the row).
     */
    private void buildInstructionsSheet(Workbook workbook, LeadImportField[] fields, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Instructions");

        Row headerRow = sheet.createRow(0);
        String[] columns = {"Column", "Required?", "Notes"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 90 * 256);

        int rowNum = 1;
        for (LeadImportField field : fields) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(field.getHeader());
            row.createCell(1).setCellValue(field.isRequired() ? "Yes" : "No");
            row.createCell(2).setCellValue(field.getInstructions());
        }

        rowNum++; // blank separator row
        Row generalNote = sheet.createRow(rowNum);
        generalNote.createCell(0).setCellValue(
                "Column headers are matched case-insensitively and don't need to match exactly — "
                        + "common variants (e.g. \"Mobile\" for Phone) are recognized automatically. "
                        + "Required columns marked with * on the Leads sheet must be present and filled "
                        + "in for a row to import; all other columns are optional and imported as-is.");

        rowNum = appendPriorityReference(sheet, rowNum + 2, headerStyle);
        appendBackgroundReference(sheet, rowNum + 1, headerStyle);
    }

    /**
     * Spells out, per tier, exactly which Priority Subcategory values are valid — generated
     * from {@link LeadPriority}/{@link LeadPrioritySubCategory} (the same enums
     * {@code LeadImportService} validates import rows against), so this can't drift out of
     * sync with what actually gets accepted. Returns the last row number written.
     */
    private int appendPriorityReference(Sheet sheet, int startRow, CellStyle headerStyle) {
        int rowNum = startRow;
        Row title = sheet.createRow(rowNum++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Priority (P1/P2/P3) and Priority Subcategory — valid values");
        titleCell.setCellStyle(headerStyle);

        for (LeadPriority tier : LeadPriority.values()) {
            Row tierRow = sheet.createRow(rowNum++);
            tierRow.createCell(0).setCellValue(tier.name() + " (" + tier.getDisplayLabel() + ")");
            String subCategoryList = tier.subCategories().stream()
                    .map(LeadPrioritySubCategory::getLabel)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            tierRow.createCell(2).setCellValue(subCategoryList);
        }
        Row note = sheet.createRow(rowNum++);
        note.createCell(0).setCellValue(
                "Priority Subcategory must belong to the tier given in the Priority column for that row "
                        + "— e.g. Priority=P1 with Priority Subcategory=\"Warm Leads\" (a P2 subcategory) "
                        + "fails the row. Matched case-insensitively.");
        return rowNum;
    }

    /** Same generated-from-the-enum approach as {@link #appendPriorityReference}, for Background. */
    private void appendBackgroundReference(Sheet sheet, int startRow, CellStyle headerStyle) {
        int rowNum = startRow;
        Row title = sheet.createRow(rowNum++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Background — valid values");
        titleCell.setCellStyle(headerStyle);

        String options = java.util.Arrays.stream(LeadBackground.values())
                .map(LeadBackground::getDisplayLabel)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        Row optionsRow = sheet.createRow(rowNum);
        optionsRow.createCell(0).setCellValue(options);
    }
}
