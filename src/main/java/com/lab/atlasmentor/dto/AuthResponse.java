package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private boolean isEmployee;
    
    public AuthResponse(String token, Long userId, String name, String email, String role, boolean isEmployee) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isEmployee = isEmployee;
    }
}
