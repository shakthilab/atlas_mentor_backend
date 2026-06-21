package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.CreateUserRequest;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.service.AdminService;
import com.lab.atlasmentor.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER') or hasRole('BRANCH_PARTNER')")
public class ManagerController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7); // Remove "Bearer " prefix
            String email = jwtService.extractUsername(token);
            
            UserResponse createdUser = adminService.createUser(request, "MANAGER");
            
            ApiResponse<UserResponse> response = ApiResponse.success("User created successfully", createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<UserResponse> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@RequestParam String role) {
        try {
            List<UserResponse> users = adminService.getUsersByRole(role.toUpperCase());
            ApiResponse<List<UserResponse>> response = ApiResponse.success("Users retrieved successfully", users);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiResponse<List<UserResponse>> response = ApiResponse.badRequest("Invalid role: " + role);
            return ResponseEntity.badRequest().body(response);
        } catch (BusinessException e) {
            ApiResponse<List<UserResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authorization) {
        try {
            UserResponse updatedUser = adminService.updateUser(userId, request, "MANAGER");
            ApiResponse<UserResponse> response = ApiResponse.success("User updated successfully", updatedUser);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<UserResponse> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
