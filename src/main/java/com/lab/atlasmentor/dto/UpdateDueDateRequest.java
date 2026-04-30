package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateDueDateRequest {
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in future")
    private LocalDate dueDate;
}
