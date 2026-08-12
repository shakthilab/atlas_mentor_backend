package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One month node in the Employee Tree (Part A2) - only ever returned for a month that
 * actually has at least one day_workspaces row for this employee (e.g. August 2026
 * onward for an employee whose template was published/instantiated then, not
 * Jan-July which never had any activity).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthNodeResponse {
    private int month;
    private String monthName;
    private int dayCount;
    /** Average of this month's days' completion %, null if nothing to average yet. */
    private Integer avgCompletionPct;
}
