package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;

public class DisputeResponseRequest {
    @NotBlank(message = "Response is required")
    private String response;
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}
