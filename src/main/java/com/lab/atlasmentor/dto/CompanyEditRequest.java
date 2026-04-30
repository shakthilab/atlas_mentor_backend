package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyEditRequest {
    
    @NotBlank(message = "Company name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    private String contactPerson;
    private String address;
    private String industry;
    private String website;
    private Long assignedTo;
}
