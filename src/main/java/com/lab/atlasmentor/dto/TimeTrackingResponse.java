package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimeTrackingResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long userId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInSeconds;
    private String durationFormatted;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
