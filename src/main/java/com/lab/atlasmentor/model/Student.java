package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import com.lab.atlasmentor.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
@EqualsAndHashCode(callSuper = true)
public class Student extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true, unique = true, referencedColumnName = "id")
    private User user;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobile_country_code_id", referencedColumnName = "id")
    private MobileCountryCode mobileCountryCode;
    
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
    
    // Additional branch fields for API convenience
    @Transient
    private Long branchId;
    
    @Transient  
    private String branchName;
    
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
    
        
    
    
}
