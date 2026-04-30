package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public class BranchStatusRequest {
    
    @NotNull(message = "Status is required")
    private UserStatus status;

    public BranchStatusRequest() {}

    public BranchStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
