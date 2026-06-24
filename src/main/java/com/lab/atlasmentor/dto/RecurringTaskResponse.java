package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.RecurringFrequency;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecurringTaskResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private RecurringFrequency frequency;
    private Integer intervalValue;
    private LocalDateTime nextExecutionTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
