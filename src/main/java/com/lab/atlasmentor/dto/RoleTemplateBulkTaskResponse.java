package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

/**
 * Result of a bulk task-clone call for one target day: which day it landed on and every task
 * that now exists on it (only the ones just created, not the day's full pre-existing task list).
 */
@Data
public class RoleTemplateBulkTaskResponse {
    private Integer dayNumber;
    private Integer month;
    private Integer year;
    private List<RoleTemplateTaskResponse> tasks;
}
