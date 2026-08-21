package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.LeadPriority;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;
import com.lab.atlasmentor.enums.LeadBackground;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private String source;
    private StudentStatus status;
    private String statusDisplayName;

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
    
    // Mobile country code object
    private MobileCountryCodeDto mobileCountryCode;
    
    // User information
    private Long userId;
    private Boolean isActive;
    
    // Tracking fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private String createdByName;
    private Long updatedBy;
    
    // Assigned by information
    private Long assignedById;
    private String assignedByName;

    // Document metadata (id, name, type) - no file bytes; use the download endpoint per doc
    private List<DocumentResponse> documents;

    // Academic history (10th/12th/Bachelor's/etc.)
    private List<AcademicHistoryResponse> academicHistory;

    // Lead priority classification (framework doc) - all optional
    private LeadPriority priority;
    private String priorityDisplayName;
    private LeadPrioritySubCategory prioritySubCategory;
    private String prioritySubCategoryDisplayName;
    private LeadBackground background;
    private String backgroundDisplayName;

    public static StudentResponse fromEntity(com.lab.atlasmentor.model.Student student) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setCourseName(student.getCourseName());
        response.setIntakePeriod(student.getIntakePeriod());
        response.setSource(student.getSource());
        response.setNotes(student.getNotes());
        response.setStatus(student.getStatus());
        response.setStatusDisplayName(student.getStatus() != null ? student.getStatus().getDisplayName() : null);
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        response.setCreatedBy(student.getCreatedBy());
        response.setUpdatedBy(student.getUpdatedBy());
        response.setPriority(student.getPriority());
        response.setPriorityDisplayName(student.getPriority() != null ? student.getPriority().getDisplayLabel() : null);
        response.setPrioritySubCategory(student.getPrioritySubCategory());
        response.setPrioritySubCategoryDisplayName(
                student.getPrioritySubCategory() != null ? student.getPrioritySubCategory().getLabel() : null);
        response.setBackground(student.getBackground());
        response.setBackgroundDisplayName(student.getBackground() != null ? student.getBackground().getDisplayLabel() : null);

        // User information
        if (student.getUser() != null) {
            response.setUserId(student.getUser().getId());
            response.setFirstName(student.getUser().getFirstName());
            response.setLastName(student.getUser().getLastName());
            response.setFullName(student.getUser().getFullName());
            response.setEmail(student.getUser().getEmail());
            response.setPhone(student.getUser().getPhone());
            response.setIsActive(student.getUser().getStatus() == com.lab.atlasmentor.enums.UserStatus.ACTIVE);
        } else {
            response.setIsActive(false);
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
        
        // Mobile country code (get from user since it's now stored in User entity)
        if (student.getUser() != null && student.getUser().getMobileCountryCode() != null) {
            response.setMobileCountryCode(new MobileCountryCodeDto(
                student.getUser().getMobileCountryCode().getId(),
                student.getUser().getMobileCountryCode().getCountryName(),
                student.getUser().getMobileCountryCode().getCountryCode(),
                student.getUser().getMobileCountryCode().getMobileCode(),
                student.getUser().getMobileCountryCode().getIsoAlpha2(),
                student.getUser().getMobileCountryCode().getIsoAlpha3(),
                student.getUser().getMobileCountryCode().getIsActive(),
                student.getUser().getMobileCountryCode().getFlagUrl(),
                student.getUser().getMobileCountryCode().getMobileNumberLength()
            ));
        } else {
            // Always set MCC object, even if null, to ensure consistent API response
            response.setMobileCountryCode(new MobileCountryCodeDto());
        }
        
        // Assigned by information
        if (student.getAssignedBy() != null) {
            response.setAssignedById(student.getAssignedBy().getId());
            response.setAssignedByName(student.getAssignedBy().getFullName());
        }
        
        return response;
    }
}
