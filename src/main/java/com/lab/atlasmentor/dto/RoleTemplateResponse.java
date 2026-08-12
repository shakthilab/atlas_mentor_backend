package com.lab.atlasmentor.dto;

import lombok.Data;
import com.lab.atlasmentor.enums.BundleStatus;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private Long roleId;
    private String roleName;
    private String roleDisplayName;
    private Long branchId;
    private String branchName;
    private BundleStatus status;
    private List<RoleTemplateDayResponse> days;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
