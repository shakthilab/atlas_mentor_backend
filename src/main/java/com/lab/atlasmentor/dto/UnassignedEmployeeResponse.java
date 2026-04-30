package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.UserStatus;
import java.time.LocalDateTime;

public class UnassignedEmployeeResponse {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private String primaryRole;
    private BranchResponse branch;
    private LocalDateTime createdAt;
    
    public UnassignedEmployeeResponse() {}
    
    public UnassignedEmployeeResponse(Long id, String name, String email, String phone, UserStatus status, 
                                    String primaryRole, BranchResponse branch, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.primaryRole = primaryRole;
        this.branch = branch;
        this.createdAt = createdAt;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public String getPrimaryRole() {
        return primaryRole;
    }
    
    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
    }
    
    public BranchResponse getBranch() {
        return branch;
    }
    
    public void setBranch(BranchResponse branch) {
        this.branch = branch;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
