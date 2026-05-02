package com.lab.atlasmentor.config;

import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();
        
        // Debug logging to help track JWT issues
        if (logger.isDebugEnabled()) {
            logger.debug("Processing request for: " + requestURI);
            logger.debug("Authorization header: " + (authorizationHeader != null ? "present" : "missing"));
        }
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            
            logger.info("JWT token extracted: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            // Check if token is empty or null after stripping "Bearer "
            if (token == null || token.trim().isEmpty()) {
                logger.warn("JWT token is empty after Bearer prefix - proceeding without authentication");
                filterChain.doFilter(request, response);
                return;
            }
            
            try {
                String email = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);
                Long userId = jwtService.extractUserId(token);
                Long branchId = jwtService.extractBranchId(token);
                
                logger.info("JWT claims extracted: email=" + email + ", role=" + role + ", userId=" + userId + ", branchId=" + branchId);
                
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Create CustomUserDetails with all claims
                    CustomUserDetails userDetails = new CustomUserDetails(userId, email, role, branchId);
                    
                    logger.info("Created CustomUserDetails: userId=" + userDetails.getUserId() + ", role=" + userDetails.getRole() + ", branchId=" + userDetails.getBranchId() + ", isAdmin=" + userDetails.isAdmin());
                    
                    // Create authentication token with CustomUserDetails as principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authentication set in SecurityContext for user: " + email);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // JWT expired - return 401 error
                logger.error("JWT token expired: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"JWT token expired\", \"message\": \"Token has expired. Please login again.\"}");
                return;
            } catch (io.jsonwebtoken.security.SignatureException e) {
                // Invalid signature - return 401 error
                logger.error("JWT signature validation failed: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid JWT signature\", \"message\": \"Token signature is invalid.\"}");
                return;
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                // Malformed token - return 401 error
                logger.error("JWT token malformed: " + e.getMessage());
                String errorMessage = "Token format is invalid.";
                if (e.getMessage().contains("Compact JWSs must contain exactly 2 period characters")) {
                    errorMessage = "Invalid JWT format. Token must contain exactly 2 period separators.";
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Malformed JWT token\", \"message\": \"" + errorMessage + "\"}");
                return;
            } catch (Exception e) {
                // Other JWT errors - return 401 error
                logger.error("JWT token validation failed: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid JWT token\", \"message\": \"Token validation failed.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
