package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import com.lab.atlasmentor.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "students",
       indexes = {
           @Index(name = "idx_students_user_id", columnList = "user_id"),
           @Index(name = "idx_students_branch_id", columnList = "branch_id"),
           @Index(name = "idx_students_assigned_by_id", columnList = "assignedBy_id"),
           @Index(name = "idx_students_status", columnList = "status"),
           @Index(name = "idx_students_enhanced_status", columnList = "enhanced_status"),
           @Index(name = "idx_students_source", columnList = "source_type, source_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
public class Student extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true, unique = true, referencedColumnName = "id")
    private User user;
    
    @Column(name = "email", length = 150)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.LEAD;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", referencedColumnName = "id")
    @JsonIgnore
    private Branch branch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignedBy_id", referencedColumnName = "id")
    private User assignedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", referencedColumnName = "id")
    private Country country;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", referencedColumnName = "id")
    private University university;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "course_name", length = 200)
    private String courseName;
    
    @Column(name = "intake_period", length = 100)
    private String intakePeriod;

    // Where the lead came from. Free text on purpose — for LeadSource.PERSON/OTHER the UI
    // collects a typed value (a referrer's name, or anything else) and that's what lands
    // here as-is; for the fixed channels (Instagram/Youtube/AD/Website) the display label
    // is stored directly. Not an @Enumerated column for exactly that reason.
    @Column(name = "source", length = 255)
    private String source;
    
    // Additional branch fields for API convenience
    @Transient
    private Long branchId;
    
    @Transient  
    private String branchName;
    
    // Created by information fields
    @Transient
    private String createdByName;
    
    @Transient
    private String createdByUserRole;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"student"})
    private java.util.List<StudentAcademicHistory> academicHistories;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"student"})
    private java.util.List<Document> documents;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;
    
    @Column(name = "source_id")
    private Long sourceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "enhanced_status")
    private StudentStatusEnhanced enhancedStatus = StudentStatusEnhanced.IN_REVIEW;

    @Column(name = "lost_reason", columnDefinition = "TEXT")
    private String lostReason;
    
    public Student() {}

    
    public Student(User user, Branch branch, User createdBy) {
        this.user = user;
        this.branch = branch;
        setCreatedBy(createdBy.getId());
    }
    
    /**
     * Get full name from associated user
     */
    public String getName() {
        return user != null ? user.getFullName() : null;
    }

    public String getLostReason() { return lostReason; }
    public void setLostReason(String lostReason) { this.lostReason = lostReason; }
    
        
    
    
}
