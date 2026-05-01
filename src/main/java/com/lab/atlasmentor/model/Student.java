package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.enums.StudentStatus;

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
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<StudentAcademicHistory> academicHistories;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Document> documents;
    
    public Student() {}

    
    public Student(User user, Branch branch, User createdBy) {
        this.user = user;
        this.branch = branch;
        setCreatedBy(createdBy);
    }
    
    /**
     * Get full name from associated user
     */
    public String getName() {
        return user != null ? user.getFullName() : null;
    }
    
        
    
    
}
