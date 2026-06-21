package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import com.lab.atlasmentor.enums.UserStatus;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"phone"}, 
                                name = "uk_users_phone")
       },
       indexes = {
           @Index(name = "idx_users_role_id", columnList = "role_id"),
           @Index(name = "idx_users_branch_id", columnList = "branch_id"),
           @Index(name = "idx_users_reporting_manager_id", columnList = "reporting_manager_id"),
           @Index(name = "idx_users_status", columnList = "status")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "email", nullable = true, unique = true, length = 150)
    private String email;
    
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Column(name = "verification_token", length = 500)
    private String verificationToken;
    
    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;
    
    @Column(name = "password_reset_token", length = 500)
    private String passwordResetToken;
    
    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    @JsonIgnoreProperties({"users", "assignedTasks"})
    private User reportingManager;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobile_country_code_id")
    private MobileCountryCode mobileCountryCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    public User() {}
    
    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }
    
    public User(String firstName, String email, String password) {
        this.firstName = firstName;
        this.email = email;
        this.password = password;
    }
    
    /**
     * Get full name (firstName + lastName)
     */
    public String getFullName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
    
    /**
     * Check if user has a specific role by name
     */
    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }
    
    /**
     * Get branch ID for backward compatibility
     */
    public Long getBranchId() {
        return branch != null ? branch.getId() : null;
    }
    
    /**
     * Set branch by ID for backward compatibility
     */
    public void setBranchId(Long branchId) {
        // This method is for backward compatibility
        // It only clears the branch when null is provided
        // Actual branch entity should be set separately in service layer
        if (branchId == null) {
            this.branch = null;
        }
        // Note: Setting the actual Branch entity should be done in the service layer
        // to avoid repository dependencies in the entity class
    }
    
    /**
     * Get reporting manager ID for hierarchy operations
     */
    public Long getReportingManagerId() {
        return reportingManager != null ? reportingManager.getId() : null;
    }
    
    /**
     * Set reporting manager by ID for backward compatibility
     */
    public void setReportingManagerId(Long reportingManagerId) {
        // This method is for backward compatibility
        // It only clears the reporting manager when null is provided
        // Actual User entity should be set separately in service layer
        if (reportingManagerId == null) {
            this.reportingManager = null;
        }
        // Note: Setting the actual User entity should be done in the service layer
        // to avoid repository dependencies in the entity class
    }
        
}
