package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskAssignmentResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long assignedById;
    private String assignedByName;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
}
