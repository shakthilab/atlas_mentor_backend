package com.lab.atlasmentor.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    
    private final Long userId;
    private final String email;
    private final String role;
    private final Long branchId;
    
    public CustomUserDetails(Long userId, String email, String role, Long branchId) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.branchId = branchId;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    
    @Override
    public String getPassword() {
        return null; // Not used for JWT authentication
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getRole() {
        return role;
    }
    
    public Long getBranchId() {
        return branchId;
    }
    
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
