package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTaskRequest {
    @NotNull(message = "Assigned to user ID is required")
    private Long assignedToId;
}
