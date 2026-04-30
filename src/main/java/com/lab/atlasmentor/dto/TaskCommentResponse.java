package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskCommentResponse {
    private Long id;
    private String comment;
    private String commentedByName;
    private LocalDateTime createdAt;
}
