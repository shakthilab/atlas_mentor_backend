package com.lab.atlasmentor.dto;

import java.util.List;
import java.util.Map;

public class StudentOnboardingRequest {

    private PersonalInfo personalInfo;
    private DestinationDetails destinationDetails;
    private List<AcademicEntry> academicHistory;
    private Map<String, String> documents;
    private String notes;
    private String referralCode;
    private String source;

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

    public List<AcademicEntry> getAcademicHistory() { return academicHistory; }
    public void setAcademicHistory(List<AcademicEntry> academicHistory) { this.academicHistory = academicHistory; }

    public Map<String, String> getDocuments() { return documents; }
    public void setDocuments(Map<String, String> documents) { this.documents = documents; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

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

    /**
     * One academic history record. The frontend sends {@code academicHistory} as a flat JSON
     * array of these (one per qualification - 10th, 12th, Bachelor's, ...), not a fixed
     * tenth/twelfth pair, so the shape here must stay a list of open-ended entries.
     * {@code id} is accepted so a future update could target a specific existing row, but the
     * current save logic (see StudentService#updateStudentData) still replaces the whole set.
     */
    public static class AcademicEntry {
        private Long id;
        private String qualification;
        private String institutionName;
        private String boardUniversity;
        private Integer passingYear;
        // Free text on purpose — StudentAcademicHistory#score is a String(50) column (percentages,
        // CGPA, grades all pass through as-is); an Integer type here would reject anything but a
        // whole number.
        private String score;
        private String stream;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getQualification() { return qualification; }
        public void setQualification(String qualification) { this.qualification = qualification; }

        public String getInstitutionName() { return institutionName; }
        public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

        public String getBoardUniversity() { return boardUniversity; }
        public void setBoardUniversity(String boardUniversity) { this.boardUniversity = boardUniversity; }

        public Integer getPassingYear() { return passingYear; }
        public void setPassingYear(Integer passingYear) { this.passingYear = passingYear; }

        public String getScore() { return score; }
        public void setScore(String score) { this.score = score; }

        public String getStream() { return stream; }
        public void setStream(String stream) { this.stream = stream; }
    }
}
