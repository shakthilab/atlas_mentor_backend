package com.lab.atlasmentor.dto;

import lombok.Data;

import java.util.List;

/**
 * Full synchronous result of one import run (Part 5 — no background job / job-status
 * endpoint for this version; the whole outcome is returned in this one response).
 */
@Data
public class LeadImportResponse {
    private String source;
    private int totalRows;
    private int succeeded;
    private int duplicates;
    private int failed;
    private List<LeadImportRowResult> results;
}
