package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import com.lab.atlasmentor.enums.DependencyType;
import lombok.Data;

@Data
public class TaskDependencyRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Depends-on task ID is required")
    private Long dependsOnTaskId;

    @NotNull(message = "Dependency type is required")
    private DependencyType dependencyType;
}
