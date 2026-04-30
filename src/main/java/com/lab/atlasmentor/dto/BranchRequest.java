package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BranchRequest {
    
    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 150, message = "Branch name must be between 2 and 150 characters")
    private String name;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    private UserStatus status = UserStatus.ACTIVE;
    
    private Long managerId;

    public BranchRequest() {}

    public BranchRequest(String name, String location, UserStatus status) {
        this.name = name;
        this.location = location;
        this.status = status != null ? status : UserStatus.ACTIVE;
    }
    
    public BranchRequest(String name, String location) {
        this.name = name;
        this.location = location;
        this.status = UserStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
}
