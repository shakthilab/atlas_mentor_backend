package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteTaskRequest {

    @NotEmpty(message = "Task IDs list cannot be empty")
    private List<Long> taskIds;
}
