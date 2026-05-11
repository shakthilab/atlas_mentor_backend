package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class StudentOnboardingRequest {
    
    // Step 1: Personal Information
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    
    @Email(message = "Email should be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    private Long mobileCountryCodeId;
    
    private Long branchId;
    
    private String status; // Lead, Active, etc.
    
    // Step 2: Destination Details
    private Long destinationCountryId;
    private Long targetUniversityId;
    private String courseName;
    private String intakePeriod;
    
    // Alias for intakePeriod to support frontend field name
    public String getIntake() {
        return intakePeriod;
    }
    
    public void setIntake(String intake) {
        this.intakePeriod = intake;
    }
    
    // Step 3: Academic History
    private List<AcademicHistory> academicHistory;
    
    // Step 4: Documents
    private Map<String, String> documents; // Key: document type, Value: base64 content
    
    // Additional fields
    private String notes;
    private String referralCode;
    private Long assignedToId; // Optional field for assigning student to a counsellor
    
    public static class AcademicHistory {
        private String level; // 10th, 12th, Graduation, etc.
        private String institutionName;
        private String passingYear;
        private String scoreCgpa;
        
        public AcademicHistory() {}
        
        public AcademicHistory(String level, String institutionName, String passingYear, String scoreCgpa) {
            this.level = level;
            this.institutionName = institutionName;
            this.passingYear = passingYear;
            this.scoreCgpa = scoreCgpa;
        }
        
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public String getInstitutionName() { return institutionName; }
        public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
        
        public String getPassingYear() { return passingYear; }
        public void setPassingYear(String passingYear) { this.passingYear = passingYear; }
        
        public String getScoreCgpa() { return scoreCgpa; }
        public void setScoreCgpa(String scoreCgpa) { this.scoreCgpa = scoreCgpa; }
    }
    
    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Long getMobileCountryCodeId() { return mobileCountryCodeId; }
    public void setMobileCountryCodeId(Long mobileCountryCodeId) { this.mobileCountryCodeId = mobileCountryCodeId; }
    
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getDestinationCountryId() { return destinationCountryId; }
    public void setDestinationCountryId(Long destinationCountryId) { this.destinationCountryId = destinationCountryId; }
    
    public Long getTargetUniversityId() { return targetUniversityId; }
    public void setTargetUniversityId(Long targetUniversityId) { this.targetUniversityId = targetUniversityId; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public String getIntakePeriod() { return intakePeriod; }
    public void setIntakePeriod(String intakePeriod) { this.intakePeriod = intakePeriod; }
    
    public List<AcademicHistory> getAcademicHistory() { return academicHistory; }
    public void setAcademicHistory(List<AcademicHistory> academicHistory) { this.academicHistory = academicHistory; }
    
    public Map<String, String> getDocuments() { return documents; }
    public void setDocuments(Map<String, String> documents) { this.documents = documents; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    
    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
}
