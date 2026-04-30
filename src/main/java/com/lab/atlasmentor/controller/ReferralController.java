package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.enums.ReferralType;
import com.lab.atlasmentor.dto.ReferralRequest;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/referral")
public class ReferralController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> createReferral(
            @Valid @RequestBody ReferralRequest referralRequest,
            HttpServletRequest request) {
        
        User referral = adminService.createReferral(referralRequest, request);
        UserResponse response = adminService.convertToUserResponse(referral);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getReferrals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String referralType,
            @RequestParam(required = false) Long branchId) {
        
        return ResponseEntity.ok(adminService.getReferrals(page, size, search, referralType, branchId));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<String>> getReferralTypes() {
        List<String> referralTypes = Arrays.stream(ReferralType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(referralTypes);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> updateReferral(
            @PathVariable Long id,
            @Valid @RequestBody ReferralRequest referralRequest) {
        
        User updatedReferral = adminService.updateReferral(id, referralRequest);
        UserResponse response = adminService.convertToUserResponse(updatedReferral);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteReferral(@PathVariable Long id) {
        adminService.deleteReferral(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> updateReferralStatus(
            @PathVariable Long id,
            @RequestParam com.lab.atlasmentor.enums.UserStatus status) {
        User updatedReferral = adminService.updateReferralStatus(id, status);
        UserResponse response = adminService.convertToUserResponse(updatedReferral);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/deactivate/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> deactivateReferral(@PathVariable Long id) {
        User deactivatedReferral = adminService.updateReferralStatus(id, com.lab.atlasmentor.enums.UserStatus.INACTIVE);
        UserResponse response = adminService.convertToUserResponse(deactivatedReferral);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/activate/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> activateReferral(@PathVariable Long id) {
        User activatedReferral = adminService.updateReferralStatus(id, com.lab.atlasmentor.enums.UserStatus.ACTIVE);
        UserResponse response = adminService.convertToUserResponse(activatedReferral);
        
        return ResponseEntity.ok(response);
    }
}
