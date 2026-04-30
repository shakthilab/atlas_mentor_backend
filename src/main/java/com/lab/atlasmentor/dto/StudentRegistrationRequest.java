package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentRegistrationRequest {
    
    // Step 1: Personal Details
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;
    
    // Step 2: Academic Preferences
    private String preferredCountry;
    private String preferredUniversity;
    private String course;
    private String intake;
    
    // Step 3: Academic Background
    private String referralCode;
    private String basicAcademicDetails;
    private String optionalNotes;

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getPreferredCountry() { return preferredCountry; }
    public void setPreferredCountry(String preferredCountry) { this.preferredCountry = preferredCountry; }
    
    public String getPreferredUniversity() { return preferredUniversity; }
    public void setPreferredUniversity(String preferredUniversity) { this.preferredUniversity = preferredUniversity; }
    
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
}
