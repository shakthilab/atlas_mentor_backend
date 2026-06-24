package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserReportingRequest {

    @NotNull(message = "Manager user ID is required")
    private Long managerUserId;

    @NotNull(message = "Employee user ID is required")
    private Long employeeUserId;

    private Long branchId;
}
