package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_notes")
@Data
public class StudentNote extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "preferred_country")
    private String preferredCountry;
    
    @Column(name = "preferred_university")
    private String preferredUniversity;
    
    @Column(name = "course")
    private String course;
    
    @Column(name = "intake")
    private String intake;
    
    @Column(name = "referral_code")
    private String referralCode;
    
    @Column(name = "academic_details", columnDefinition = "TEXT")
    private String academicDetails;
    
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;
    
    public StudentNote() {}
    
    public StudentNote(Long studentId, String preferredCountry, String preferredUniversity, 
                       String course, String intake, String referralCode, 
                       String academicDetails, String additionalNotes, User createdBy) {
        this.studentId = studentId;
        this.preferredCountry = preferredCountry;
        this.preferredUniversity = preferredUniversity;
        this.course = course;
        this.intake = intake;
        this.referralCode = referralCode;
        this.academicDetails = academicDetails;
        this.additionalNotes = additionalNotes;
        setCreatedBy(createdBy);
    }
    
        
}
