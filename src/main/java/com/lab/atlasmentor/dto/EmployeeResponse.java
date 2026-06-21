package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Long branchId;
    private BranchDto branch;
    private String status;
    private Boolean isVerified;
    private RoleDto role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private MobileCountryCodeDto mobileCountryCode;
    private TaskCount taskCount;
    private ManagerDto manager;

    public EmployeeResponse() {}

    public EmployeeResponse(Long id, String firstName, String lastName, String email, String phone, Long branchId, BranchDto branch,
                         String status, Boolean isVerified, RoleDto role,
                         LocalDateTime createdAt, LocalDateTime updatedAt, Long createdBy, Long updatedBy,
                         MobileCountryCodeDto mobileCountryCode, TaskCount taskCount) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.branchId = branchId;
        this.branch = branch;
        this.status = status;
        this.isVerified = isVerified;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.mobileCountryCode = mobileCountryCode;
        this.taskCount = taskCount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public BranchDto getBranch() { return branch; }
    public void setBranch(BranchDto branch) { this.branch = branch; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public RoleDto getRole() { return role; }
    public void setRole(RoleDto role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public MobileCountryCodeDto getMobileCountryCode() { return mobileCountryCode; }
    public void setMobileCountryCode(MobileCountryCodeDto mobileCountryCode) { this.mobileCountryCode = mobileCountryCode; }

    public TaskCount getTaskCount() { return taskCount; }
    public void setTaskCount(TaskCount taskCount) { this.taskCount = taskCount; }

    public ManagerDto getManager() { return manager; }
    public void setManager(ManagerDto manager) { this.manager = manager; }

    // ManagerDto nested class
    public static class ManagerDto {
        private Long id;
        private String name;

        public ManagerDto() {}

        public ManagerDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // TaskCount nested class
    public static class TaskCount {
        private Long pending;
        private Long inProgress;
        private Long completed;

        public TaskCount() {}

        public TaskCount(Long pending, Long inProgress, Long completed) {
            this.pending = pending;
            this.inProgress = inProgress;
            this.completed = completed;
        }

        public Long getPending() { return pending; }
        public void setPending(Long pending) { this.pending = pending; }

        public Long getInProgress() { return inProgress; }
        public void setInProgress(Long inProgress) { this.inProgress = inProgress; }

        public Long getCompleted() { return completed; }
        public void setCompleted(Long completed) { this.completed = completed; }
    }

    // RoleDto nested class
    public static class RoleDto {
        private Long id;
        private String name;
        private String description;

        public RoleDto() {}

        public RoleDto(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // BranchDto nested class
    public static class BranchDto {
        private Long id;
        private String name;
        private String location;
        private String status;

        public BranchDto() {}

        public BranchDto(Long id, String name, String location, String status) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // MobileCountryCodeDto nested class
    public static class MobileCountryCodeDto {
        private Long id;
        private String countryName;
        private String countryCode;
        private String mobileCode;
        private String isoAlpha2;
        private String isoAlpha3;
        private Boolean isActive;
        private String flagUrl;
        private Integer mobileNumberLength;

        public MobileCountryCodeDto() {}

        public MobileCountryCodeDto(Long id, String countryName, String countryCode, String mobileCode,
                                  String isoAlpha2, String isoAlpha3, Boolean isActive, String flagUrl,
                                  Integer mobileNumberLength) {
            this.id = id;
            this.countryName = countryName;
            this.countryCode = countryCode;
            this.mobileCode = mobileCode;
            this.isoAlpha2 = isoAlpha2;
            this.isoAlpha3 = isoAlpha3;
            this.isActive = isActive;
            this.flagUrl = flagUrl;
            this.mobileNumberLength = mobileNumberLength;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCountryName() { return countryName; }
        public void setCountryName(String countryName) { this.countryName = countryName; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getMobileCode() { return mobileCode; }
        public void setMobileCode(String mobileCode) { this.mobileCode = mobileCode; }

        public String getIsoAlpha2() { return isoAlpha2; }
        public void setIsoAlpha2(String isoAlpha2) { this.isoAlpha2 = isoAlpha2; }

        public String getIsoAlpha3() { return isoAlpha3; }
        public void setIsoAlpha3(String isoAlpha3) { this.isoAlpha3 = isoAlpha3; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public String getFlagUrl() { return flagUrl; }
        public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }

        public Integer getMobileNumberLength() { return mobileNumberLength; }
        public void setMobileNumberLength(Integer mobileNumberLength) { this.mobileNumberLength = mobileNumberLength; }
    }
}
