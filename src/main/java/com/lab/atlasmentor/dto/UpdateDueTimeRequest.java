package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpdateDueTimeRequest {
    @NotNull(message = "Due time is required")
    private LocalDateTime dueTime;
}
