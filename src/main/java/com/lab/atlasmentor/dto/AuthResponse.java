package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String roleName;
    private boolean isEmployee;

    public AuthResponse(String token, String refreshToken, Long userId, String name, String email, String role, String roleName, boolean isEmployee) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.roleName = roleName;
        this.isEmployee = isEmployee;
    }
}
