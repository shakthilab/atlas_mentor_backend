package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.AuthResponse;
import com.lab.atlasmentor.dto.LoginRequest;
import com.lab.atlasmentor.dto.RefreshTokenRequest;
import com.lab.atlasmentor.dto.RegisterRequest;
import com.lab.atlasmentor.dto.ResetPasswordRequest;
import com.lab.atlasmentor.exception.TooManyRequestsException;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.security.LoginRateLimiter;
import com.lab.atlasmentor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = authService.register(registerRequest);
            ApiResponse<User> response = ApiResponse.success(
                "Registration successful. Please check your email for verification link.",
                user
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<User> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        String ip = extractClientIp(request);
        if (!loginRateLimiter.isAllowed(ip)) {
            long retryAfter = loginRateLimiter.retryAfterSeconds(ip);
            return tooManyRequests("Too many login attempts. Try again in " + retryAfter + " seconds.", retryAfter);
        }
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
        } catch (TooManyRequestsException e) {
            return tooManyRequests(e.getMessage(), e.getRetryAfterSeconds());
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestBody Map<String, String> body) {
        try {
            authService.resendVerificationEmail(body.get("email"));
            return ResponseEntity.ok(ApiResponse.success("Verification email sent successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
        }
        String ip = extractClientIp(request);
        if (!loginRateLimiter.isAllowed(ip)) {
            long retryAfter = loginRateLimiter.retryAfterSeconds(ip);
            return tooManyRequests("Too many requests. Try again in " + retryAfter + " seconds.", retryAfter);
        }
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest body) {
        try {
            AuthResponse authResponse = authService.refresh(body.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "401"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest body) {
        authService.logout(body.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody ResetPasswordRequest body,
            HttpServletRequest request) {
        String ip = extractClientIp(request);
        if (!loginRateLimiter.isAllowed(ip)) {
            long retryAfter = loginRateLimiter.retryAfterSeconds(ip);
            return tooManyRequests("Too many requests. Try again in " + retryAfter + " seconds.", retryAfter);
        }
        try {
            authService.resetPassword(body.getToken(), body.getPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private <T> ResponseEntity<ApiResponse<T>> tooManyRequests(String message, long retryAfterSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(retryAfterSeconds));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(ApiResponse.error(message, "429"));
    }
}
