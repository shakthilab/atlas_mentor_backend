package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCommentResponse {
    private Long id;
    private String comment;
    private Long commentedById;
    private String commentedByName;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private boolean edited;

    /** Nested replies (task_comments rows whose parent_comment_id = this comment's id). */
    private List<TaskCommentResponse> replies;
}
