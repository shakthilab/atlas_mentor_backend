package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private Long referenceId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
