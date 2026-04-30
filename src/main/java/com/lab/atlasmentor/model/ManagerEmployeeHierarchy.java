package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "manager_employee_hierarchy", 
       uniqueConstraints = @UniqueConstraint(columnNames = "employee_id"))
@Data
public class ManagerEmployeeHierarchy extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "manager_id", nullable = false)
    private Long managerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", referencedColumnName = "id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User manager;
    
    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User employee;
    
    public ManagerEmployeeHierarchy() {}
    
    public ManagerEmployeeHierarchy(Long managerId, Long employeeId) {
        this.managerId = managerId;
        this.employeeId = employeeId;
    }
    
}
