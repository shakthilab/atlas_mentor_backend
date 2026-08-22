package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;

public class StudentSourceUpdateRequest {

    @NotBlank(message = "Source is required")
    private String source;

    public StudentSourceUpdateRequest() {}

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
