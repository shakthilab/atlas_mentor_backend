package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeAcceptRequest {
    
    @NotBlank(message = "Acceptance notes are required")
    private String acceptanceNotes;
    
    public DisputeAcceptRequest() {}
    
    public DisputeAcceptRequest(String acceptanceNotes) {
        this.acceptanceNotes = acceptanceNotes;
    }
}
