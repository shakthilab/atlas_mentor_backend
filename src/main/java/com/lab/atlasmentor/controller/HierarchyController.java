package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.CounsellorAssignmentRequest;
import com.lab.atlasmentor.dto.CounsellorHierarchyResponse;
import com.lab.atlasmentor.dto.EmployeeAssignmentRequest;
import com.lab.atlasmentor.dto.JuniorCounsellorResponse;
import com.lab.atlasmentor.dto.ManagerEmployeeAssignmentRequest;
import com.lab.atlasmentor.dto.ManagerHierarchyResponse;
import com.lab.atlasmentor.dto.RoleRequest;
import com.lab.atlasmentor.dto.SeniorCounsellorResponse;
import com.lab.atlasmentor.model.ManagerEmployeeHierarchy;
import com.lab.atlasmentor.model.ReferralAssignment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.security.SecurityUtils;
import com.lab.atlasmentor.service.HierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyController {

    @Autowired
    private HierarchyService hierarchyService;

    @Autowired
    private ManagerEmployeeHierarchyRepository managerEmployeeHierarchyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounsellorHierarchyRepository counsellorHierarchyRepository;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private ReferralDetailsRepository referralDetailsRepository;

    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;

    @Autowired
    private ReferralAssignmentRepository referralAssignmentRepository;

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<ManagerHierarchyResponse>>> getManagerHierarchy() {
        try {
            List<ManagerHierarchyResponse> managerHierarchy = hierarchyService.getManagerHierarchy();
            ApiResponse<List<ManagerHierarchyResponse>> response = ApiResponse.success("Manager hierarchy retrieved successfully", managerHierarchy);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ManagerHierarchyResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/counsellors")
    public ResponseEntity<ApiResponse<List<CounsellorHierarchyResponse>>> getCounsellorHierarchy() {
        try {
            List<CounsellorHierarchyResponse> counsellorHierarchy = hierarchyService.getCounsellorHierarchy();
            ApiResponse<List<CounsellorHierarchyResponse>> response = ApiResponse.success("Counsellor hierarchy retrieved successfully", counsellorHierarchy);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<CounsellorHierarchyResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/assign-employee")
    public ResponseEntity<ApiResponse<String>> assignEmployeeToManager(@RequestBody ManagerEmployeeAssignmentRequest request) {
        try {
            // Check if employee is already assigned to a manager
            if (managerEmployeeHierarchyRepository.existsByEmployeeId(request.getEmployeeId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Employee is already assigned to a manager"));
            }
            
            ManagerEmployeeHierarchy assignment = new ManagerEmployeeHierarchy(
                request.getManagerId(), 
                request.getEmployeeId()
            );
            managerEmployeeHierarchyRepository.save(assignment);
            
            return ResponseEntity.ok(ApiResponse.success("Employee assigned to manager successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/assign-employee-by-roles")
    public ResponseEntity<ApiResponse<String>> assignEmployeesToManagerByRoles(@RequestBody EmployeeAssignmentRequest request) {
        try {
            // Get current user
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            // Validate that manager exists
            User manager = userRepository.findById(request.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));
            
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errorMessages = new StringBuilder();
            
            for (Long userId : request.getUserIds()) {
                try {
                    // Validate that user exists and has the specified role
                    User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
                    
                    // Check if user has the specified role
                    boolean userHasRole = user.getRole() != null && user.getRole().getId().equals(request.getRoleId());

                    if (!userHasRole) {
                        errorMessages.append("User ").append(userId).append(" does not have the specified role. ");
                        failureCount++;
                        continue;
                    }

                    // Junior counsellors cannot be assigned directly to a manager
                    if (user.hasRole("JUNIOR_COUNSELLOR")) {
                        errorMessages.append("User ").append(userId).append(" is a Junior Counsellor and cannot be assigned directly to a Manager. Junior Counsellors must be assigned through a Senior Counsellor. ");
                        failureCount++;
                        continue;
                    }
                    
                    hierarchyService.assignEmployeeToManager(user, manager, currentUser);
                    successCount++;
                    
                } catch (Exception e) {
                    errorMessages.append("Error assigning user ").append(userId).append(": ").append(e.getMessage()).append(". ");
                    failureCount++;
                }
            }

            String message = String.format("Assignment completed. Successfully assigned %d employees, %d failed.", successCount, failureCount);
            if (failureCount > 0) {
                message += " Errors: " + errorMessages.toString();
                return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
            }

            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/manager-of-employee/{employeeId}")
    public ResponseEntity<ApiResponse<Long>> getManagerOfEmployee(@PathVariable Long employeeId) {
        try {
            Long managerId = managerEmployeeHierarchyRepository.findManagerIdByEmployeeId(employeeId);
            if (managerId == null) {
                return ResponseEntity.ok(ApiResponse.success("Employee is not assigned to any manager", null));
            }
            return ResponseEntity.ok(ApiResponse.success("Manager retrieved successfully", managerId));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/assign-junior-counsellors")
    public ResponseEntity<ApiResponse<String>> assignJuniorCounsellorsToSenior(@RequestBody CounsellorAssignmentRequest request) {
        try {
            // Validate that senior counsellor exists
            User seniorCounsellor = userRepository.findById(request.getSeniorCounsellorId())
                .orElseThrow(() -> new RuntimeException("Senior counsellor not found"));
            
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errorMessages = new StringBuilder();
            
            for (Long juniorCounsellorId : request.getJuniorCounsellorIds()) {
                try {
                    // Validate that junior counsellor exists
                    // Check if junior counsellor is already assigned to a senior counsellor
                    if (counsellorHierarchyRepository.findByJuniorCounsellor_Id(juniorCounsellorId).isPresent()) {
                        errorMessages.append("Junior counsellor ").append(juniorCounsellorId).append(" is already assigned to a senior counsellor. ");
                        failureCount++;
                        continue;
                    }
                    
                                        com.lab.atlasmentor.model.User juniorCounsellor = userRepository.findById(juniorCounsellorId)
                        .orElseThrow(() -> new RuntimeException("Junior counsellor not found with ID: " + juniorCounsellorId));
                    
                    com.lab.atlasmentor.model.CounsellorHierarchy assignment = new com.lab.atlasmentor.model.CounsellorHierarchy(
                        seniorCounsellor, 
                        juniorCounsellor
                    );
                    counsellorHierarchyRepository.save(assignment);
                    successCount++;
                    
                } catch (BusinessException e) {
                    errorMessages.append("Error assigning junior counsellor ").append(juniorCounsellorId).append(": ").append(e.getMessage()).append(". ");
                    failureCount++;
                }
            }
            
            String message = String.format("Assignment completed. Successfully assigned %d junior counsellors, %d failed.", successCount, failureCount);
            if (failureCount > 0) {
                message += " Errors: " + errorMessages.toString();
                return ResponseEntity.badRequest().body(ApiResponse.error(message));
            }
            
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/unassign-junior-counsellor/{juniorCounsellorId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> unassignJuniorCounsellorFromSenior(@PathVariable Long juniorCounsellorId) {
        try {
            counsellorHierarchyRepository.deleteByJuniorCounsellor_Id(juniorCounsellorId);
            return ResponseEntity.ok(ApiResponse.success("Junior counsellor unassigned from senior counsellor successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/unassign-employee/{employeeId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> unassignEmployeeFromManager(@PathVariable Long employeeId) {
        try {
            // Also clear assignedTo fields in various detail tables
            userRepository.findById(employeeId).ifPresent(user -> {
                if (user.hasRole("COMPANY")) {
                    companyDetailsRepository.findByUserId(employeeId).ifPresent(companyDetails -> {
                        companyDetails.setAssignedTo(null);
                        companyDetailsRepository.save(companyDetails);
                    });
                }
                
                if (user.hasRole("REFERRAL")) {
                    // Remove all referral assignments for this referral
                    referralAssignmentRepository.deleteByReferralId(employeeId);
                }
                
                // Clear EmployeeDetails.assignedTo if the user has employee details
                employeeDetailsRepository.findByUserId(employeeId).ifPresent(employeeDetails -> {
                    employeeDetails.setAssignedTo(null);
                    employeeDetailsRepository.save(employeeDetails);
                });
            });
            
            managerEmployeeHierarchyRepository.deleteByEmployeeId(employeeId);
            return ResponseEntity.ok(ApiResponse.success("Employee unassigned from manager successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/senior-counsellor-of-junior/{juniorCounsellorId}")
    public ResponseEntity<ApiResponse<Long>> getSeniorCounsellorOfJunior(@PathVariable Long juniorCounsellorId) {
        try {
            java.util.Optional<Long> seniorCounsellorId = counsellorHierarchyRepository.findSeniorCounsellorIdByJuniorCounsellorId(juniorCounsellorId);
            if (seniorCounsellorId.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Junior counsellor is not assigned to any senior counsellor", null));
            }
            return ResponseEntity.ok(ApiResponse.success("Senior counsellor retrieved successfully", seniorCounsellorId.get()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users-by-role")
    public ResponseEntity<ApiResponse<List<SeniorCounsellorResponse>>> getUsersByRole(@RequestParam String role, @RequestParam(required = false) Long branchId) {
        try {
            List<User> users = userRepository.findByRoleNames(List.of(role));
            
            // Filter by branch if branchId is provided
            if (branchId != null) {
                users = users.stream()
                    .filter(user -> user.getBranchId() != null && user.getBranchId().equals(branchId))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Get all employee IDs that are assigned to managers
            List<Long> assignedEmployeeIds = managerEmployeeHierarchyRepository.findAll().stream()
                    .map(ManagerEmployeeHierarchy::getEmployeeId)
                    .collect(java.util.stream.Collectors.toList());
            
            // Get all junior counsellor IDs that are assigned to senior counsellors
            List<Long> assignedJuniorCounsellorIds = counsellorHierarchyRepository.findAll().stream()
                    .map(ch -> ch.getJuniorCounsellor().getId())
                    .collect(java.util.stream.Collectors.toList());
            
            // Combine all assigned IDs
            List<Long> allAssignedIds = new java.util.ArrayList<>(assignedEmployeeIds);
            allAssignedIds.addAll(assignedJuniorCounsellorIds);
            
            // Filter out users who are already assigned (to managers or senior counsellors)
            List<User> unassignedUsers = users.stream()
                    .filter(user -> !allAssignedIds.contains(user.getId()))
                    .collect(java.util.stream.Collectors.toList());
            
            List<SeniorCounsellorResponse> userResponses = unassignedUsers.stream()
                .map(user -> new SeniorCounsellorResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getBranchId(),
                    user.getStatus().toString(),
                    user.getIsVerified(),
                    user.getRole().getName()
                ))
                .collect(java.util.stream.Collectors.toList());
            
            String message = branchId != null ? 
                "Unassigned users retrieved successfully for role " + role + " and branch " + branchId : 
                "Unassigned users retrieved successfully for role " + role;
            
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.success(message, userResponses);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/junior-counsellors")
    public ResponseEntity<ApiResponse<List<JuniorCounsellorResponse>>> getAllJuniorCounsellors(@RequestParam(required = false) Long branchId) {
        try {
            List<User> juniorCounsellors = userRepository.findAllJuniorCounsellors();
            
            // Filter by branch if branchId is provided
            if (branchId != null) {
                juniorCounsellors = juniorCounsellors.stream()
                    .filter(user -> user.getBranchId() != null && user.getBranchId().equals(branchId))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            List<JuniorCounsellorResponse> juniorCounsellorResponses = juniorCounsellors.stream()
                .map(user -> new JuniorCounsellorResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getBranchId(),
                    user.getStatus().toString(),
                    user.getIsVerified(),
                    user.getRole() // Get the Role object
                ))
                .collect(java.util.stream.Collectors.toList());
            
            String message = branchId != null ? 
                "Junior counsellors retrieved successfully for branch " + branchId : 
                "Junior counsellors retrieved successfully";
            
            ApiResponse<List<JuniorCounsellorResponse>> response = ApiResponse.success(message, juniorCounsellorResponses);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<JuniorCounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/users-by-role")
    public ResponseEntity<ApiResponse<List<SeniorCounsellorResponse>>> getUsersByRoleWithObject(@RequestBody RoleRequest roleRequest) {
        try {
            // First find users by role names, then filter to get only those with the specified role
            List<User> allUsers = userRepository.findByRoleNames(List.of("SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "MANAGER"));
            List<User> filteredUsers = allUsers.stream()
                .filter(user -> (roleRequest.getId() != null && user.getRole() != null && user.getRole().getId().intValue() == roleRequest.getId()) || 
                              (roleRequest.getName() != null && user.hasRole(roleRequest.getName())))
                .collect(java.util.stream.Collectors.toList());
            
            List<SeniorCounsellorResponse> userResponses = filteredUsers.stream()
                .map(user -> new SeniorCounsellorResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getBranchId(),
                    user.getStatus().toString(),
                    user.getIsVerified(),
                    user.getRole().getName()
                ))
                .collect(java.util.stream.Collectors.toList());
            
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.success("Users retrieved successfully", userResponses);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
