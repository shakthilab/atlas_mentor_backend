package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskAttachmentResponse {

    private Long id;
    private Long taskId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileSizeFormatted;
    /** Size in bytes before server-side compression (V36) - null for attachments registered without a real upload. */
    private Long originalFileSize;
    /** IMAGE / VIDEO / AUDIO / DOCUMENT, derived from the file name - see AttachmentTypeUtil. */
    private String fileType;
    /** Which comment this attachment is attached to, or null when it's a direct proof-section attachment on the task. */
    private Long commentId;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
