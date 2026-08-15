package com.lab.atlasmentor.dto;

import lombok.Data;

/**
 * Both fields are optional. {@code newTemplateName} blank/omitted -> "<source name> Copy",
 * same convention as {@link DuplicateTemplateRequest} (Role Templates). {@code newCycleMonth}
 * ("yyyy-MM") blank/omitted -> same cycle month as the source - unlike Role Templates, a
 * Weekly Accountability template is month-scoped, so re-targeting the month on duplicate (e.g.
 * cloning August's template into September) is the common case this adds over the Role
 * Template equivalent.
 */
@Data
public class WeeklyAccountabilityDuplicateTemplateRequest {
    private String newTemplateName;
    private String newCycleMonth;
}
