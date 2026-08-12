package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class RoleTemplateDayRequest {
    private Long id;

    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    private Boolean isWeeklyCheckpoint = false;

    private List<RoleTemplateTaskRequest> tasks;
}
