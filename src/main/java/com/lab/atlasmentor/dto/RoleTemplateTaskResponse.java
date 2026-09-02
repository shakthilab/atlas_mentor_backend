package com.lab.atlasmentor.dto;

import lombok.Data;
import com.lab.atlasmentor.enums.Priority;

@Data
public class RoleTemplateTaskResponse {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private Integer displayOrder;
    private Boolean proofRequired;
}
