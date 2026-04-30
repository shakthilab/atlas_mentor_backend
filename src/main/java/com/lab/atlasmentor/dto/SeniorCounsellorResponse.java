package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class SeniorCounsellorResponse {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Long branchId;
    private String status;
    private Boolean isVerified;
    private String role;
    
    public SeniorCounsellorResponse(Long id, String name, String email, String phone, 
                                   Long branchId, String status, Boolean isVerified, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.branchId = branchId;
        this.status = status;
        this.isVerified = isVerified;
        this.role = role;
    }
}
