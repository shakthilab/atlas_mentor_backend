package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimeTrackingRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationInSeconds;
}
