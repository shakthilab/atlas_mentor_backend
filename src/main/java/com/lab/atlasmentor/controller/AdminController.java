package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.CreateUserRequest;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.model.User;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7);
            String email = jwtService.extractUsername(token);
            String roleStr = jwtService.extractRole(token);
            
            // Only ADMIN users can create other users
            if (!"ADMIN".equals(roleStr.toUpperCase())) {
                ApiResponse<UserResponse> response = ApiResponse.badRequest("Only ADMIN users can create users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            UserResponse createdUser = adminService.createUser(request, roleStr.toUpperCase());
            
            ApiResponse<UserResponse> response = ApiResponse.success("User created successfully", createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<UserResponse> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        try {
            List<UserResponse> users = adminService.getAllUsers();
            ApiResponse<List<UserResponse>> response = ApiResponse.success("Users retrieved successfully", users);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<UserResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable String role) {
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

    @GetMapping("/get-all-employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_COUNSELLOR')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersExcludingAdminAndStudent(
            @RequestParam(required = false) List<Long> roleIds,
            @RequestParam(required = false) Long branchId) {
        try {
            List<UserResponse> users = adminService.getUsersExcludingAdminAndStudent(roleIds, branchId);
            ApiResponse<List<UserResponse>> response = ApiResponse.success("Users retrieved successfully", users);
            return ResponseEntity.ok(response);
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
            String token = authorization.substring(7);
            String roleStr = jwtService.extractRole(token);
            
            // Only ADMIN users can update other users
            if (!"ADMIN".equals(roleStr.toUpperCase())) {
                ApiResponse<UserResponse> response = ApiResponse.badRequest("Only ADMIN users can update users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            UserResponse updatedUser = adminService.updateUser(userId, request, roleStr.toUpperCase());
            ApiResponse<UserResponse> response = ApiResponse.success("User updated successfully", updatedUser);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<UserResponse> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            ApiResponse<Void> response = ApiResponse.success("User deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<Void> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
