package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CountryRequest {
    
    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Country code is required")
    @Size(max = 5, message = "Country code must not exceed 5 characters")
    private String code;
}
