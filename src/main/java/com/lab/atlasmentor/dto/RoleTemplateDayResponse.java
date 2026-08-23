package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleTemplateDayResponse {
    private Long id;
    private Integer dayNumber;
    private Boolean isWeeklyCheckpoint;
    private Integer month;
    private Integer year;
    private List<RoleTemplateTaskResponse> tasks;
}
