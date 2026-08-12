package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Priority priority;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in future")
    private LocalDate dueDate;

    /** Optional deadline timestamp, independent of dueDate - not required. */
    private LocalDateTime dueTime;

    @NotNull(message = "Assigned to user ID is required")
    private Long assignedToId;

    private Long branchId;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    private String referenceType;

    private Long referenceId;
}
