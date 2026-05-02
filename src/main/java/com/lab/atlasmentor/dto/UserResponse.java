package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.ReferralType;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private Boolean isVerified;
    private BranchResponse branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ReferralType referralType;
    private CompanyDetailsResponse companyDetails;
    private String assignedToUsername;
    private UserCounts userCounts;
    
    public UserResponse(Long id, String firstName, String lastName, String email, String phone, Role role, 
                       UserStatus status, Boolean isVerified, BranchResponse branch, 
                       LocalDateTime createdAt, LocalDateTime updatedAt, ReferralType referralType, 
                       CompanyDetailsResponse companyDetails, String assignedToUsername, UserCounts userCounts) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.isVerified = isVerified;
        this.branch = branch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.referralType = referralType;
        this.companyDetails = companyDetails;
        this.assignedToUsername = assignedToUsername;
        this.userCounts = userCounts;
    }
    
    // UserCounts nested class
    public static class UserCounts {
        private Long totalStaffs;
        private Long totalStudents;

        public UserCounts() {}

        public UserCounts(Long totalStaffs, Long totalStudents) {
            this.totalStaffs = totalStaffs;
            this.totalStudents = totalStudents;
        }

        public Long getTotalStaffs() { return totalStaffs; }
        public void setTotalStaffs(Long totalStaffs) { this.totalStaffs = totalStaffs; }

        public Long getTotalStudents() { return totalStudents; }
        public void setTotalStudents(Long totalStudents) { this.totalStudents = totalStudents; }
    }
}
