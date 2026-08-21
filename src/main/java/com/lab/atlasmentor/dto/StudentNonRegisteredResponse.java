package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.LeadPriority;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;
import com.lab.atlasmentor.enums.LeadBackground;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentNonRegisteredResponse {
    private Long id;
    private String notes;
    private String courseName;
    private String intakePeriod;
    private String source;
    private String status;

    // Lead priority classification (framework doc) - all optional
    private LeadPriority priority;
    private String priorityDisplayName;
    private LeadPrioritySubCategory prioritySubCategory;
    private String prioritySubCategoryDisplayName;
    private LeadBackground background;
    private String backgroundDisplayName;

    // Branch information
    private Long branchId;
    private String branchName;
    private String branchLocation;
    
    // Country information
    private Long countryId;
    private String countryName;
    
    // University information
    private Long universityId;
    private String universityName;
    
    // Mobile country code object
    private MobileCountryCodeDto mobileCountryCode;
    
    // User information (nested user object)
    private UserDto user;
    
    // Tracking fields
    private String createdAt;
    private Long createdBy;
    private Long updatedBy;
    
    // Assigned by information (full user object)
    private UserDto assignedBy;
    
    // Created by information (full user object)
    private UserDto createdByUser;
    
    // Nested user DTO class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private Long branchId;
        private String branchName;
        private String createdAt;
        private String role;
    }
}
