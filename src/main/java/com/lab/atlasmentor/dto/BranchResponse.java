package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.UserStatus;
import java.time.LocalDateTime;

public class BranchResponse {
    
    private Long id;
    private String name;
    private String location;
    private UserStatus status;
    private LocalDateTime createdAt;
    private ManagerInfo manager;
    private UserCounts userCounts;
    
    public static class ManagerInfo {
        private Long id;
        private String name;
        private String email;
        
        public ManagerInfo() {}
        
        public ManagerInfo(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
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
    }

    public BranchResponse() {}

    public BranchResponse(Long id, String name, String location, UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
    }

    public BranchResponse(Long id, String name, String location, UserStatus status, LocalDateTime createdAt, UserCounts userCounts) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
        this.userCounts = userCounts;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ManagerInfo getManager() {
        return manager;
    }

    public void setManager(ManagerInfo manager) {
        this.manager = manager;
    }

    public UserCounts getUserCounts() {
        return userCounts;
    }

    public void setUserCounts(UserCounts userCounts) {
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
