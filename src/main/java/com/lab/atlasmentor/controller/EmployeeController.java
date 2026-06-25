package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.EmployeeEditRequest;
import com.lab.atlasmentor.dto.EmployeeRequest;
import com.lab.atlasmentor.dto.EmployeeResponse;
import com.lab.atlasmentor.dto.EmployeeStatusUpdateRequest;
import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long branch,
            @RequestParam(required = false) String search) {
        try {
            String roleStr = role != null ? role : null;
            PageResponse<EmployeeResponse> employees = authService.getAllEmployees(page, size, roleStr, branch, search);
            if (employees.isEmpty()) {
                ApiResponse<PageResponse<EmployeeResponse>> response = ApiResponse.success("No data found", employees);
                return ResponseEntity.ok(response);
            }
            ApiResponse<PageResponse<EmployeeResponse>> response = ApiResponse.success("Employees retrieved successfully", employees);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<PageResponse<EmployeeResponse>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> addEmployee(@Valid @RequestBody EmployeeRequest employeeRequest, 
                                                   HttpServletRequest request) {
        try {
            EmployeeResponse employee = authService.createEmployee(employeeRequest, request);
            ApiResponse<EmployeeResponse> response = ApiResponse.success("Employee created successfully", employee);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<EmployeeResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> editEmployee(@PathVariable Long id, 
                                                        @Valid @RequestBody EmployeeEditRequest editRequest) {
        try {
            EmployeeResponse updatedEmployee = authService.editEmployee(id, editRequest);
            ApiResponse<EmployeeResponse> response = ApiResponse.success("Employee updated successfully", updatedEmployee);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<EmployeeResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateEmployeeStatus(@PathVariable Long id, 
                                                                   @Valid @RequestBody EmployeeStatusUpdateRequest statusRequest) {
        try {
            String action = authService.updateEmployeeStatus(id, statusRequest.getStatus());
            String message;
            String successMessage;
            
            if (action.equals("already_active")) {
                message = "Employee with ID " + id + " is already active and can login";
                successMessage = "Employee already active";
            } else if (action.equals("already_inactive")) {
                message = "Employee with ID " + id + " is already inactive and cannot login";
                successMessage = "Employee already inactive";
            } else if (action.equals("active")) {
                message = "Employee with ID " + id + " has been activated and can now login";
                successMessage = "Employee activated successfully";
            } else if (action.equals("inactive")) {
                message = "Employee with ID " + id + " has been deactivated and can no longer login";
                successMessage = "Employee deactivated successfully";
            } else {
                message = "Employee with ID " + id + " status updated to " + statusRequest.getStatus();
                successMessage = "Employee status updated successfully";
            }
            
            ApiResponse<String> response = ApiResponse.success(successMessage, message);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        try {
            authService.deleteEmployee(id);
            ApiResponse<String> response = ApiResponse.success("Employee deleted successfully", "Employee with ID " + id + " has been permanently deleted");
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
