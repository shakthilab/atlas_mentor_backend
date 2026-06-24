package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskWatcherResponse {

    private Long id;
    private Long taskId;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;
}
