package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Registers a file that the client already uploaded elsewhere (e.g. to blob/object
 * storage) against a task - task_attachments.file_url is a reference, not raw content
 * (unlike Document.base64Content), so there is no server-side file upload here.
 */
@Data
public class AddAttachmentRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Long fileSize;

    /** Optional - links this attachment to a specific comment (task_comments.id). */
    private Long commentId;
}
