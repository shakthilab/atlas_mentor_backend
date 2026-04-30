package com.lab.atlasmentor.dto;

import java.util.List;

public class CounsellorHierarchyResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private BranchDto branch;
    private List<JuniorCounsellorDto> juniorCounsellors;

    public CounsellorHierarchyResponse() {}

    public CounsellorHierarchyResponse(Long id, String name, String email, String phone, 
                                     BranchDto branch, List<JuniorCounsellorDto> juniorCounsellors) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.branch = branch;
        this.juniorCounsellors = juniorCounsellors;
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

    public List<JuniorCounsellorDto> getJuniorCounsellors() { return juniorCounsellors; }
    public void setJuniorCounsellors(List<JuniorCounsellorDto> juniorCounsellors) { this.juniorCounsellors = juniorCounsellors; }

    // JuniorCounsellorDto nested class
    public static class JuniorCounsellorDto {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String status;
        private Long studentsAssigned;

        public JuniorCounsellorDto() {}

        public JuniorCounsellorDto(Long id, String name, String email, String phone, 
                                  String status, Long studentsAssigned) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.status = status;
            this.studentsAssigned = studentsAssigned;
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

        public Long getStudentsAssigned() { return studentsAssigned; }
        public void setStudentsAssigned(Long studentsAssigned) { this.studentsAssigned = studentsAssigned; }
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
