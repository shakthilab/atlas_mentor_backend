package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentStatus;
import jakarta.validation.constraints.NotNull;

public class StudentStatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private StudentStatus status;
    
    private String notes;
    
    public StudentStatusUpdateRequest() {}
    
    public StudentStatusUpdateRequest(StudentStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }
    
    public StudentStatus getStatus() {
        return status;
    }
    
    public void setStatus(StudentStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
