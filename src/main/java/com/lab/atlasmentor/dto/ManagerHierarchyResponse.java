package com.lab.atlasmentor.dto;

import java.util.List;

public class ManagerHierarchyResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private BranchDto branch;
    private List<EmployeeDto> employees;

    public ManagerHierarchyResponse() {}

    public ManagerHierarchyResponse(Long id, String name, String email, String phone, 
                                  BranchDto branch, List<EmployeeDto> employees) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.branch = branch;
        this.employees = employees;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BranchDto getBranch() { return branch; }
    public void setBranch(BranchDto branch) { this.branch = branch; }

    public List<EmployeeDto> getEmployees() { return employees; }
    public void setEmployees(List<EmployeeDto> employees) { this.employees = employees; }

    // EmployeeDto nested class
    public static class EmployeeDto {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String status;
        private String role;

        public EmployeeDto() {}

        public EmployeeDto(Long id, String name, String email, String phone, 
                         String status, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.status = status;
            this.role = role;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
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
}
