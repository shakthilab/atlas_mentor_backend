package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePriorityRequest {
    @NotNull(message = "Priority is required")
    private Priority priority;
}
