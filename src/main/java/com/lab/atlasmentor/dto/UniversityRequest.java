package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class UniversityRequest {
    
    @NotBlank(message = "University name is required")
    @Size(max = 200, message = "University name must not exceed 200 characters")
    private String name;
    
    @NotNull(message = "Country ID is required")
    private Long countryId;
}
