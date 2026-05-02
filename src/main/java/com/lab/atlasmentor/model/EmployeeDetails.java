package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import com.lab.atlasmentor.enums.EmployeeType;

@Entity
@Table(name = "employee_details")
@Data
public class EmployeeDetails extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, referencedColumnName = "id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false)
    private EmployeeType employeeType;
    
    @Column(name = "is_senior", nullable = false)
    private Boolean isSenior = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", referencedColumnName = "id")
    private User manager;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", referencedColumnName = "id")
    private User assignedTo;
    
    public EmployeeDetails() {}
    
    public EmployeeDetails(User user, EmployeeType employeeType, User manager) {
        this.user = user;
        this.employeeType = employeeType;
        this.manager = manager;
    }
    
}
