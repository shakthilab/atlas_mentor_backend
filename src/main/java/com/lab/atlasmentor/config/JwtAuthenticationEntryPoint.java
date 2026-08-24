package com.lab.atlasmentor.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs whenever a request reaches a secured endpoint with no valid authentication at all -
 * no Authorization header, or one that isn't a "Bearer ..." value - and Spring Security's
 * access-control layer rejects it. Without this bean, Spring Security falls back to its
 * default entry point, which answers with a bare 403 and no body.
 *
 * 403 there is misleading: it should mean "authenticated, but not allowed" (that case is
 * handled separately - see AccessDeniedException in GlobalExceptionHandler, and the
 * WWW-Authenticate-driven 401 responses JwtAuthenticationFilter already returns for a token
 * that's present but expired/invalid/malformed). This is the "not authenticated at all" case,
 * so it gets the same 401 treatment as every other authentication failure in this app - a
 * client can branch on "log in again" for one status instead of two.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication is required to access this resource.\"}");
    }
}
