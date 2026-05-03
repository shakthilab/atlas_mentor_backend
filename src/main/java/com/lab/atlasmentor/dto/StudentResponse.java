package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String notes;
    private String courseName;
    private String intakePeriod;
    private StudentStatus status;
    
    // Branch information - simplified to avoid circular references
    private Long branchId;
    private String branchName;
    private String branchLocation;
    
    // Country information
    private Long countryId;
    private String countryName;
    
    // University information
    private Long universityId;
    private String universityName;
    
    // Mobile country code
    private Long mobileCountryCodeId;
    private String mobileCountryCode;
    private String mobileCountryCodeSymbol;
    
    // User information
    private Long userId;
    
    // Tracking fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    // Assigned by information
    private Long assignedById;
    private String assignedByName;
    
    public static StudentResponse fromEntity(com.lab.atlasmentor.model.Student student) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setCourseName(student.getCourseName());
        response.setIntakePeriod(student.getIntakePeriod());
        response.setNotes(student.getNotes());
        response.setStatus(student.getStatus());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        response.setCreatedBy(student.getCreatedBy());
        response.setUpdatedBy(student.getUpdatedBy());
        
        // User information
        if (student.getUser() != null) {
            response.setUserId(student.getUser().getId());
            response.setFirstName(student.getUser().getFirstName());
            response.setLastName(student.getUser().getLastName());
            response.setFullName(student.getUser().getFullName());
            response.setEmail(student.getUser().getEmail());
            response.setPhone(student.getUser().getPhone());
        }
        
        // Branch information
        if (student.getBranch() != null) {
            response.setBranchId(student.getBranch().getId());
            response.setBranchName(student.getBranch().getName());
            response.setBranchLocation(student.getBranch().getLocation());
        }
        
        // Country information
        if (student.getCountry() != null) {
            response.setCountryId(student.getCountry().getId());
            response.setCountryName(student.getCountry().getName());
        }
        
        // University information
        if (student.getUniversity() != null) {
            response.setUniversityId(student.getUniversity().getId());
            response.setUniversityName(student.getUniversity().getName());
        }
        
        // Mobile country code
        if (student.getMobileCountryCode() != null) {
            response.setMobileCountryCodeId(student.getMobileCountryCode().getId());
            response.setMobileCountryCode(student.getMobileCountryCode().getCountryCode());
            response.setMobileCountryCodeSymbol(student.getMobileCountryCode().getMobileCode());
        }
        
        // Assigned by information
        if (student.getAssignedBy() != null) {
            response.setAssignedById(student.getAssignedBy().getId());
            response.setAssignedByName(student.getAssignedBy().getFullName());
        }
        
        return response;
    }
}
