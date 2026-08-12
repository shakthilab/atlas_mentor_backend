package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCommentRequest {
    @NotBlank(message = "Comment is required")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    private String comment;

    /**
     * Optional - set to reply to an existing comment (task_comments.parent_comment_id).
     * Null for a top-level comment.
     */
    private Long parentCommentId;
}
