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
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
