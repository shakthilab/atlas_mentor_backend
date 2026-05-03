package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.BranchRequest;
import com.lab.atlasmentor.dto.BranchResponse;
import com.lab.atlasmentor.dto.BranchStatusRequest;
import com.lab.atlasmentor.dto.ManagerResponse;
import com.lab.atlasmentor.dto.SeniorCounsellorResponse;
import com.lab.atlasmentor.dto.UnassignedEmployeeResponse;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.service.BranchService;
import com.lab.atlasmentor.service.AdminService;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @Autowired
    private AdminService adminService;

    // SecurityUtil removed - now using SecurityUtils directly

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentRepository studentRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody BranchRequest branchRequest) {
        try {
            // Validate admin role using SecurityUtils
            if (!SecurityUtils.isCurrentUserAdmin()) {
                throw new RuntimeException("Access denied. Admin role required.");
            }
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            Branch branch = new Branch();
            branch.setName(branchRequest.getName());
            branch.setLocation(branchRequest.getLocation());
            branch.setStatus(branchRequest.getStatus() != null ? branchRequest.getStatus() : UserStatus.ACTIVE);
            branch.setCreatedBy(currentUser.getId());
            branch.setUpdatedBy(currentUser.getId());
            
            Branch createdBranch = branchService.createBranch(branch, branchRequest.getManagerId());
            BranchResponse response = convertToBranchResponse(createdBranch);
            
            ApiResponse<BranchResponse> apiResponse = ApiResponse.success("Branch created successfully", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (RuntimeException e) {
            ApiResponse<BranchResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Long id) {
        try {
            Optional<Branch> branch = branchService.getBranchById(id);
            if (branch.isPresent()) {
                BranchResponse response = convertToBranchResponse(branch.get());
                ApiResponse<BranchResponse> apiResponse = ApiResponse.success("Branch retrieved successfully", response);
                return ResponseEntity.ok(apiResponse);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            ApiResponse<BranchResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        try {
            List<Branch> branches;
            if (includeInactive) {
                branches = branchService.getAllBranchesIncludingInactive();
            } else {
                branches = branchService.getAllBranches();
            }
            List<BranchResponse> branchResponses = branches.stream()
                    .map(this::convertToBranchResponse)
                    .collect(Collectors.toList());
            ApiResponse<List<BranchResponse>> response = ApiResponse.success("Branches retrieved successfully", branchResponses);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<List<BranchResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id, 
            @Valid @RequestBody BranchRequest branchRequest) {
        try {
            // Validate admin role using SecurityUtils
            if (!SecurityUtils.isCurrentUserAdmin()) {
                throw new RuntimeException("Access denied. Admin role required.");
            }
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            Branch branchDetails = new Branch();
            branchDetails.setName(branchRequest.getName());
            branchDetails.setLocation(branchRequest.getLocation());
            branchDetails.setStatus(branchRequest.getStatus());
            branchDetails.setUpdatedBy(currentUser.getId());
            
            Branch updatedBranch = branchService.updateBranch(id, branchDetails, branchRequest.getManagerId());
            BranchResponse response = convertToBranchResponse(updatedBranch);
            
            ApiResponse<BranchResponse> apiResponse = ApiResponse.success("Branch updated successfully", response);
            return ResponseEntity.ok(apiResponse);
        } catch (RuntimeException e) {
            ApiResponse<BranchResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            // Validate admin role using SecurityUtils
            if (!SecurityUtils.isCurrentUserAdmin()) {
                throw new RuntimeException("Access denied. Admin role required.");
            }
            branchService.deleteBranch(id);
            ApiResponse<Void> response = ApiResponse.success("Branch deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BranchResponse>> changeBranchStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody BranchStatusRequest statusRequest) {
        try {
            // Validate admin role using SecurityUtils
            if (!SecurityUtils.isCurrentUserAdmin()) {
                throw new RuntimeException("Access denied. Admin role required.");
            }
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            Branch updatedBranch = branchService.changeBranchStatus(id, statusRequest.getStatus(), currentUser);
            
            BranchResponse response = convertToBranchResponse(updatedBranch);
            ApiResponse<BranchResponse> apiResponse = ApiResponse.success("Branch status updated successfully", response);
            return ResponseEntity.ok(apiResponse);
        } catch (RuntimeException e) {
            ApiResponse<BranchResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private BranchResponse convertToBranchResponse(Branch branch) {
        // Get staff and student counts for the branch
        List<String> staffRoles = List.of("MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR");
        
        Long totalStaffs = userRepository.countStaffsByBranchId(branch.getId(), staffRoles);
        Long totalStudents = studentRepository.countStudentsByBranchId(branch.getId());
        
        BranchResponse.UserCounts userCounts = new BranchResponse.UserCounts(totalStaffs, totalStudents);
        
        BranchResponse response = new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getLocation(),
                branch.getStatus(),
                branch.getCreatedAt(),
                userCounts
        );
        
        // Set manager information if present
        if (branch.getManager() != null) {
            BranchResponse.ManagerInfo managerInfo = new BranchResponse.ManagerInfo(
                    branch.getManager().getId(),
                    branch.getManager().getFullName(),
                    branch.getManager().getEmail()
            );
            response.setManager(managerInfo);
        }
        
        return response;
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<ManagerResponse>>> getAllManagers() {
        try {
            List<ManagerResponse> managers = adminService.getAllManagers();
            ApiResponse<List<ManagerResponse>> response = ApiResponse.success("Managers retrieved successfully", managers);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<List<ManagerResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/senior-counsellors")
    public ResponseEntity<ApiResponse<List<SeniorCounsellorResponse>>> getAllActiveSeniorCounsellors() {
        try {
            List<SeniorCounsellorResponse> seniorCounsellors = adminService.getAllActiveSeniorCounsellors();
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.success("Senior counsellors retrieved successfully", seniorCounsellors);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<List<SeniorCounsellorResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/unassigned-employees")
    public ResponseEntity<ApiResponse<List<UnassignedEmployeeResponse>>> getUnassignedEmployees() {
        try {
            List<UnassignedEmployeeResponse> unassignedEmployees = adminService.getUnassignedEmployees();
            ApiResponse<List<UnassignedEmployeeResponse>> response = ApiResponse.success("Unassigned employees retrieved successfully", unassignedEmployees);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<List<UnassignedEmployeeResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
