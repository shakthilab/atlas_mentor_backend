package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.CounsellorResponse;
import com.lab.atlasmentor.dto.ReferralCompanyUserResponse;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/counsellors/by-branch")
    public ResponseEntity<ApiResponse<List<CounsellorResponse>>> getCounsellorsByBranch(
            @RequestParam Long branchId) {
        try {
            List<CounsellorResponse> counsellors = userService.getCounsellorsByBranch(branchId);
            if (counsellors.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", counsellors));
            }
            ApiResponse<List<CounsellorResponse>> response = ApiResponse.success(
                "Counsellors retrieved successfully", counsellors);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<CounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/counsellors/active")
    public ResponseEntity<ApiResponse<List<CounsellorResponse>>> getActiveCounsellorsByBranch(
            @RequestParam Long branchId) {
        try {
            List<CounsellorResponse> counsellors = userService.getActiveCounsellorsByBranch(branchId);
            if (counsellors.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", counsellors));
            }
            ApiResponse<List<CounsellorResponse>> response = ApiResponse.success(
                "Active counsellors retrieved successfully", counsellors);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<CounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/active-by-roles-and-branch")
    public ResponseEntity<ApiResponse<List<ReferralCompanyUserResponse>>> getActiveUsersByRoleIdsAndBranch(
            @RequestParam List<Long> roleIds,
            @RequestParam Long branchId) {
        try {
            List<ReferralCompanyUserResponse> users = userService.getActiveReferralsAndCompaniesByBranch(roleIds, branchId);
            if (users.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", users));
            }
            ApiResponse<List<ReferralCompanyUserResponse>> response = ApiResponse.success(
                "Active users retrieved successfully", users);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ReferralCompanyUserResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/active-by-role-and-branch")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getActiveUsersByRoleIdAndBranch(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long branchId) {
        try {
            List<UserResponse> users = userService.getActiveUsersByRoleIdAndBranchId(roleId, branchId);
            if (users.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", users));
            }
            ApiResponse<List<UserResponse>> response = ApiResponse.success(
                "Active users retrieved successfully", users);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<UserResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
