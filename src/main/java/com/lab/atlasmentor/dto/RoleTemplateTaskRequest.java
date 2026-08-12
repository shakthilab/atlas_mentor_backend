package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.lab.atlasmentor.enums.Priority;

@Data
public class RoleTemplateTaskRequest {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Priority priority = Priority.MEDIUM;

    private Integer displayOrder = 0;
}
