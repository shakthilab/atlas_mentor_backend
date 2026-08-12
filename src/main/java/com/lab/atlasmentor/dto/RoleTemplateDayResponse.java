package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleTemplateDayResponse {
    private Long id;
    private Integer dayNumber;
    private Boolean isWeeklyCheckpoint;
    private List<RoleTemplateTaskResponse> tasks;
}
