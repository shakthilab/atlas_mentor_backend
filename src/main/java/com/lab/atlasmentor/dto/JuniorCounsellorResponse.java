package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.Role;
import lombok.Data;

@Data
public class JuniorCounsellorResponse {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Long branchId;
    private String status;
    private Boolean isVerified;
    private Role role;
    
    public JuniorCounsellorResponse(Long id, String name, String email, String phone, 
                                   Long branchId, String status, Boolean isVerified, Role role) {
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
