package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One year node in the Employee Tree (Part A2) - only ever returned for a year that
 * actually has at least one day_workspaces row for this employee. The frontend tree
 * should render only what this endpoint returns (no empty Jan/Feb placeholder years);
 * see the recommendation on GET /api/admin/employees/{employeeId}/years.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearNodeResponse {
    private int year;
    private int dayCount;
}
