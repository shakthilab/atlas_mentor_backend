package com.lab.atlasmentor.dto;

import lombok.Data;

/**
 * DTO for role response.
 */
@Data
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private String displayName;
    private Boolean isEmployee;
}
