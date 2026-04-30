package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.TaskAction;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityResponse {
    private Long id;
    private TaskAction action;
    private String message;
    private String oldValue;
    private String newValue;
    private String doneByName;
    private LocalDateTime createdAt;
}
