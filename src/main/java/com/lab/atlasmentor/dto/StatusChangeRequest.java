package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StatusChangeRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "New status is required")
    private StudentStatusEnhanced newStatus;
    
    @NotNull(message = "Reason is required")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;
    
    @Size(max = 500, message = "Proof URL must not exceed 500 characters")
    private String proofUrl;
}
