package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.CompanyEditRequest;
import com.lab.atlasmentor.dto.CompanyRequest;
import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<UserResponse> createCompany(
            @Valid @RequestBody CompanyRequest companyRequest,
            HttpServletRequest request) {

        User company = adminService.createCompany(companyRequest, request);
        UserResponse response = adminService.convertToUserResponse(company);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long branchId) {

        PageResponse<UserResponse> companies = adminService.getCompanies(page, size, search, branchId);
        if (companies.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No data found", companies));
        }
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<UserResponse> getCompanyById(@PathVariable Long id) {

        User company = adminService.getCompanyById(id);
        UserResponse response = adminService.convertToUserResponse(company);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<UserResponse> editCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyEditRequest editRequest) {

        User updatedCompany = adminService.updateCompany(id, editRequest);
        UserResponse response = adminService.convertToUserResponse(updatedCompany);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/toggle-status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<UserResponse> toggleCompanyStatus(@PathVariable Long id) {

        User company = adminService.toggleCompanyStatus(id);
        UserResponse response = adminService.convertToUserResponse(company);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {

        adminService.deleteCompany(id);

        return ResponseEntity.ok().body("{\"message\": \"Company deleted successfully\"}");
    }
}
