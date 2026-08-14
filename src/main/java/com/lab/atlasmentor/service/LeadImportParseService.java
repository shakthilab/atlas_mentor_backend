package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.LeadImportRawRow;
import com.lab.atlasmentor.enums.LeadImportField;
import com.lab.atlasmentor.exception.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes any of the three supported input formats (.xlsx, .xls, .csv — whether from a
 * direct upload or a resolved share link) into one common row structure, {@link LeadImportRawRow},
 * already column-mapped against {@link LeadImportField} (Part 3's auto-detection: canonical
 * header first, alias list as fallback). Everything downstream of this class is format-agnostic.
 */
@Service
public class LeadImportParseService {

    /** .xlsx / .xls, via Apache POI. WorkbookFactory auto-detects which of the two it is. */
    public List<LeadImportRawRow> parseExcel(InputStream in) {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new BusinessException("The file has no rows.");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = rowIterator.next();
            Map<Integer, LeadImportField> columnMap = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                LeadImportField field = matchField(stripRequiredMarker(formatter.formatCellValue(cell)));
                if (field != null) {
                    columnMap.put(cell.getColumnIndex(), field);
                }
            }
            validateRequiredHeadersPresent(columnMap.values());

            List<LeadImportRawRow> rows = new ArrayList<>();
            int rowNumber = 1; // header is row 1, matching what the employee sees in Excel
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNumber++;
                Map<LeadImportField, String> values = new EnumMap<>(LeadImportField.class);
                for (Map.Entry<Integer, LeadImportField> entry : columnMap.entrySet()) {
                    Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cell != null ? formatter.formatCellValue(cell).trim() : null;
                    if (value != null && !value.isEmpty()) {
                        values.put(entry.getValue(), value);
                    }
                }
                if (!values.isEmpty()) {
                    rows.add(new LeadImportRawRow(rowNumber, values));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new BusinessException("Could not read the file — make sure it's a valid .xlsx or .xls file.");
        }
    }

    /** .csv, whether a direct upload or a Google Sheets CSV export fetched by LeadLinkResolverService. */
    public List<LeadImportRawRow> parseCsv(Reader reader) {
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build()
                .parse(reader)) {

            Map<String, LeadImportField> headerNameMap = new LinkedHashMap<>();
            for (String rawHeader : parser.getHeaderNames()) {
                LeadImportField field = matchField(stripRequiredMarker(rawHeader));
                if (field != null) {
                    headerNameMap.put(rawHeader, field);
                }
            }
            validateRequiredHeadersPresent(headerNameMap.values());

            List<LeadImportRawRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                Map<LeadImportField, String> values = new EnumMap<>(LeadImportField.class);
                for (Map.Entry<String, LeadImportField> entry : headerNameMap.entrySet()) {
                    String value = record.isSet(entry.getKey()) ? record.get(entry.getKey()) : null;
                    if (value != null && !value.trim().isEmpty()) {
                        values.put(entry.getValue(), value.trim());
                    }
                }
                if (!values.isEmpty()) {
                    rows.add(new LeadImportRawRow(rowNumber, values));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new BusinessException("Could not read the file as CSV — the format may be corrupted.");
        }
    }

    private LeadImportField matchField(String rawHeader) {
        for (LeadImportField field : LeadImportField.values()) {
            if (field.matchesHeader(rawHeader)) {
                return field;
            }
        }
        return null;
    }

    /** Our own generated template appends " *" to required column headers — strip it before matching. */
    private String stripRequiredMarker(String header) {
        return header == null ? null : header.replace("*", "").trim();
    }

    private void validateRequiredHeadersPresent(Collection<LeadImportField> matchedFields) {
        List<String> missing = new ArrayList<>();
        for (LeadImportField field : LeadImportField.values()) {
            if (field.isRequired() && !matchedFields.contains(field)) {
                missing.add(field.getHeader());
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("The file is missing required column(s): " + String.join(", ", missing)
                    + ". Download the template from GET /api/leads/import/template and match its header row.");
        }
    }
}
