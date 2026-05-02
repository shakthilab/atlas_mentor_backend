package com.lab.atlasmentor.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authenticated user found in security context");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new RuntimeException("Invalid principal type in security context. Expected CustomUserDetails, got: " 
                + principal.getClass().getSimpleName());
        }
        
        return (CustomUserDetails) principal;
    }
    
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
    
    public static String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }
    
    public static Long getCurrentBranchId() {
        return getCurrentUser().getBranchId();
    }
    
    public static boolean isCurrentUserAdmin() {
        return getCurrentUser().isAdmin();
    }
}
