package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskListResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private Integer displayOrder;
    private Long taskBundleId;
    private String taskBundleName;
    private Long taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
