package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.DependencyType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskDependencyResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long dependsOnTaskId;
    private String dependsOnTaskTitle;
    private DependencyType dependencyType;
    private LocalDateTime createdAt;
}
