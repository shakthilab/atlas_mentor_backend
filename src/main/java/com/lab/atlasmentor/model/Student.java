package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import com.lab.atlasmentor.enums.StudentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
public class Student extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "email", length = 150)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.LEAD;
    
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
//
//    @Column(name = "assigned_counsellor_id")
//    private Long assignedCounsellorId;
//
//    @Column(name = "referral_id")
//    private Long referralId;
//

    @Column(name = "assignedBy_id")
    private Long assignedBy;


    @Column(name = "company_id")
    private Long companyId;
    
    public Student() {}
    
    public Student(String firstName, String lastName, Long branchId, User createdBy) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.branchId = branchId;
        setCreatedBy(createdBy);
    }
    
    public Student(String firstName, Long branchId, User createdBy) {
        this.firstName = firstName;
        this.branchId = branchId;
        setCreatedBy(createdBy);
    }
    
    /**
     * Get full name (firstName + lastName)
     */
    public String getName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
    
    /**
     * Set full name - splits into firstName and lastName
     */
    public void setName(String fullName) {
        if (fullName != null) {
            String[] nameParts = fullName.split(" ", 2);
            this.firstName = nameParts[0];
            this.lastName = nameParts.length > 1 ? nameParts[1] : null;
        }
    }
    
        
    
    
}
