package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "user_reporting",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"manager_user_id", "employee_user_id"},
                   name = "uk_user_reporting_manager_employee")
       },
       indexes = {
           @Index(name = "idx_user_reporting_manager", columnList = "manager_user_id"),
           @Index(name = "idx_user_reporting_employee", columnList = "employee_user_id"),
           @Index(name = "idx_user_reporting_branch", columnList = "branch_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserReporting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({"users"})
    private Branch branch;

    public UserReporting() {}

    public UserReporting(User manager, User employee, Branch branch) {
        this.manager = manager;
        this.employee = employee;
        this.branch = branch;
    }
}
