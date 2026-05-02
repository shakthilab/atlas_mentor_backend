package com.lab.atlasmentor.util;

import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    public String extractRoleFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing authorization token");
        }
        
        String jwt = token.substring(7);
        String role = jwtService.extractRole(jwt);
        
        if (role == null || role.trim().isEmpty()) {
            throw new RuntimeException("Invalid user role in token");
        }
        
        return role.toUpperCase();
    }

    public String extractEmailFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing authorization token");
        }
        
        String jwt = token.substring(7);
        return jwtService.extractUsername(jwt);
    }

    public Long extractUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing authorization token");
        }
        
        String jwt = token.substring(7);
        return jwtService.extractUserId(jwt);
    }

    public Long extractBranchIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing authorization token");
        }
        
        String jwt = token.substring(7);
        return jwtService.extractBranchId(jwt);
    }

    public void validateAdminRole(String token) {
        String role = extractRoleFromToken(token);
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
    }

    public User extractUserFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing authorization token");
        }
        
        Long userId = extractUserIdFromToken(token);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }
}
