package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCommentRequest {
    /**
     * Optional - null/blank for a voice-only comment: the client creates the
     * comment first to get a commentId, then attaches the recording via
     * POST /api/tasks/{id}/attachments/upload with that commentId.
     */
    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;

    /**
     * Optional - set to reply to an existing comment (task_comments.parent_comment_id).
     * Null for a top-level comment.
     */
    private Long parentCommentId;
}

