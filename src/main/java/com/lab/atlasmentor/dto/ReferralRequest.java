package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ReferralType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReferralRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotNull(message = "Referral type is required")
    private ReferralType referralType;
    
    private Long branchId; // Optional: Branch ID to assign the referral to
    
    private String institutionName; // For COACHING_CENTER type
    private Integer graduationYear; // For ALUMNI type
    private String employeeId; // For EMPLOYEE referral type
}
