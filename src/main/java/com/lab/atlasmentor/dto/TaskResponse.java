package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.enums.TaskSource;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private String assigneeName;
    private String assignerName;
    private LocalDate dueDate;
    private LocalDateTime dueTime;
    private LocalDate executionDate;
    private TaskSource sourceType;
    private String branchName;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean isDeleted;
    
    // Additional fields for better API response
    private Long assignedToId;
    private Long assignedById;
    private Long taskBundleId;
    private String taskBundleName;
    private Long branchId;
}
