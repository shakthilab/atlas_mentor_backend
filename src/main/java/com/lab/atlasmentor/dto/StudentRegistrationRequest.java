package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentRegistrationRequest {
    
    // Step 1: Personal Details
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    private Long mobileCountryCodeId;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;
    
    // Step 2: Academic Preferences
    private Long countryId;
    private Long universityId;
    private String course;
    private String intake;
    
    // Step 3: Academic Background
    private String referralCode;
    private String basicAcademicDetails;
    private String optionalNotes;
    
    // Student Notes
    private String notes;

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
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Long getCountryId() { return countryId; }
    public void setCountryId(Long countryId) { this.countryId = countryId; }
    
    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getIntake() { return intake; }
    public void setIntake(String intake) { this.intake = intake; }
    
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    
    public String getBasicAcademicDetails() { return basicAcademicDetails; }
    public void setBasicAcademicDetails(String basicAcademicDetails) { this.basicAcademicDetails = basicAcademicDetails; }
    
    public String getOptionalNotes() { return optionalNotes; }
    public void setOptionalNotes(String optionalNotes) { this.optionalNotes = optionalNotes; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
