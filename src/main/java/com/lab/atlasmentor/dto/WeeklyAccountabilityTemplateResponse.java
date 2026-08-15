package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeeklyAccountabilityTemplateResponse {
    private Long id;
    private String name;
    private Long roleId;
    private String roleName;
    private String roleDisplayName;
    private LocalDate cycleMonth;
    private BundleStatus status;
    private List<WeeklyAccountabilityWeekResponse> weeks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
