package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class RoleTemplateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Role ID is required")
    private Long roleId;

    private Long branchId;

    private List<RoleTemplateDayRequest> days;
}
