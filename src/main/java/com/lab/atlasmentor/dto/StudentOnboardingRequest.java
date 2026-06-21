package com.lab.atlasmentor.dto;

import java.util.Map;

public class StudentOnboardingRequest {

    private PersonalInfo personalInfo;
    private DestinationDetails destinationDetails;
    private AcademicHistoryWrapper academicHistory;
    private Map<String, String> documents;
    private String notes;
    private String referralCode;

    // ── Convenience getters used by StudentService ────────────────────────────

    public String getFirstName() { return personalInfo != null ? personalInfo.getFirstName() : null; }
    public String getLastName()  { return personalInfo != null ? personalInfo.getLastName()  : null; }
    public String getEmail()     { return personalInfo != null ? personalInfo.getEmail()     : null; }
    public String getPhone()     { return personalInfo != null ? personalInfo.getPhone()     : null; }
    public Long   getBranchId()  { return personalInfo != null ? personalInfo.getBranchId() : null; }
    public Long   getAssignedToId() { return personalInfo != null ? personalInfo.getCounsellor() : null; }
    public Long   getMobileCountryCodeId() { return personalInfo != null ? personalInfo.getMobileCountryCodeId() : null; }
    public String getStatus()    { return null; }

    public Long   getDestinationCountryId() { return destinationDetails != null ? destinationDetails.getCountryId()    : null; }
    public Long   getTargetUniversityId()   { return destinationDetails != null ? destinationDetails.getUniversityId() : null; }
    public String getCourseName()           { return destinationDetails != null ? destinationDetails.getCourse()       : null; }
    public String getIntakePeriod()         { return destinationDetails != null ? destinationDetails.getIntake()       : null; }

    // ── Top-level field getters/setters ───────────────────────────────────────

    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }

    public DestinationDetails getDestinationDetails() { return destinationDetails; }
    public void setDestinationDetails(DestinationDetails destinationDetails) { this.destinationDetails = destinationDetails; }

    public AcademicHistoryWrapper getAcademicHistory() { return academicHistory; }
    public void setAcademicHistory(AcademicHistoryWrapper academicHistory) { this.academicHistory = academicHistory; }

    public Map<String, String> getDocuments() { return documents; }
    public void setDocuments(Map<String, String> documents) { this.documents = documents; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class PersonalInfo {
        private String firstName;
        private String lastName;
        private String countryCode;
        private String phone;
        private String email;
        private Long branchId;
        private Long counsellor;
        private Long mobileCountryCodeId;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Long getBranchId() { return branchId; }
        public void setBranchId(Long branchId) { this.branchId = branchId; }

        public Long getCounsellor() { return counsellor; }
        public void setCounsellor(Long counsellor) { this.counsellor = counsellor; }

        public Long getMobileCountryCodeId() { return mobileCountryCodeId; }
        public void setMobileCountryCodeId(Long mobileCountryCodeId) { this.mobileCountryCodeId = mobileCountryCodeId; }
    }

    public static class DestinationDetails {
        private Long countryId;
        private Long universityId;
        private String course;
        private String intake;

        public Long getCountryId() { return countryId; }
        public void setCountryId(Long countryId) { this.countryId = countryId; }

        public Long getUniversityId() { return universityId; }
        public void setUniversityId(Long universityId) { this.universityId = universityId; }

        public String getCourse() { return course; }
        public void setCourse(String course) { this.course = course; }

        public String getIntake() { return intake; }
        public void setIntake(String intake) { this.intake = intake; }
    }

    public static class AcademicEntry {
        private String institution;
        private Integer year;
        private Integer score;

        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }

    public static class AcademicHistoryWrapper {
        private AcademicEntry tenth;
        private AcademicEntry twelfth;

        public AcademicEntry getTenth() { return tenth; }
        public void setTenth(AcademicEntry tenth) { this.tenth = tenth; }

        public AcademicEntry getTwelfth() { return twelfth; }
        public void setTwelfth(AcademicEntry twelfth) { this.twelfth = twelfth; }
    }
}
