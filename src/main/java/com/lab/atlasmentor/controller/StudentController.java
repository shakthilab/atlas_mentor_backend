package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.lab.atlasmentor.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Student>> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        try {
            Student student = studentService.registerStudent(request);
            ApiResponse<Student> response = ApiResponse.success(
                "Student registration successful. We will contact you soon.", 
                student
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            ApiResponse<Student> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable Long id) {
        try {
            StudentResponse student = studentService.getStudentByIdAsResponse(id);
            ApiResponse<StudentResponse> response = ApiResponse.success("Student retrieved successfully", student);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            ApiResponse<String> response = ApiResponse.success("Student and all related data deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> getAllStudents(
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<StudentResponse> students = studentService.getAllStudents(status, search, pageable);
        ApiResponse<PageResponse<StudentResponse>> response = ApiResponse.success("Students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registered")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> getRegisteredStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<StudentResponse> students = studentService.getAllStudents(StudentStatus.REGISTERED, search, pageable);
        ApiResponse<PageResponse<StudentResponse>> response = ApiResponse.success("Registered students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/non-registered")
    public ResponseEntity<ApiResponse<PageResponse<StudentNonRegisteredResponse>>> getNonRegisteredStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String countryName,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<StudentNonRegisteredResponse> students = studentService.getNonRegisteredStudents(search, status, countryName, dateFrom, dateTo, source, pageable);
        ApiResponse<PageResponse<StudentNonRegisteredResponse>> response = ApiResponse.<PageResponse<StudentNonRegisteredResponse>>success("Non-registered students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByEmail(@PathVariable String email) {
        try {
            Student student = studentService.findStudentByEmail(email);
            if (student != null) {
                StudentResponse studentResponse = StudentResponse.fromEntity(student);
                ApiResponse<StudentResponse> response = (ApiResponse<StudentResponse>) ApiResponse.success("Student found successfully", studentResponse);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<StudentResponse> response = ApiResponse.success("No student found with this email", null);
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            ApiResponse<StudentResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<String>> createOrUpdateStudent(@Valid @RequestBody StudentOnboardingRequest request) {
        try {
            Student student = studentService.createOrUpdateStudent(request);
            String message = student.getUser() != null ? "Student updated successfully" : "Student onboarded successfully";
            ApiResponse<String> response = ApiResponse.success(message, null);
            return ResponseEntity.status(student.getUser() != null ? HttpStatus.OK : HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            // Provide specific error messages for common scenarios
            if (errorMessage.contains("Email already exists")) {
                errorMessage = "A user with this email already exists. Please use a different email address.";
            } else if (errorMessage.contains("Mobile country code not found")) {
                errorMessage = "Invalid mobile country code selected.";
            } else if (errorMessage.contains("Branch not found")) {
                errorMessage = "Invalid branch selected.";
            } else if (errorMessage.contains("Country not found")) {
                errorMessage = "Invalid destination country selected.";
            } else if (errorMessage.contains("University not found")) {
                errorMessage = "Invalid target university selected.";
            } else if (errorMessage.contains("Invalid passing year format")) {
                errorMessage = "Invalid passing year format. Please provide a valid year.";
            } else if (errorMessage.contains("Failed to create student")) {
                errorMessage = "Student onboarding failed. Please check all fields and try again.";
            }
            ApiResponse<String> response = ApiResponse.error(errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentOnboardingRequest request) {
        try {
            Student student = studentService.updateStudent(id, request);
            StudentResponse studentResponse = StudentResponse.fromEntity(student);
            ApiResponse<StudentResponse> response = ApiResponse.success("Student updated successfully", studentResponse);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudentStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusUpdateRequest request) {
        try {
            Student student = studentService.updateStudentStatus(id, request);
            StudentResponse studentResponse = StudentResponse.fromEntity(student);
            ApiResponse<StudentResponse> response = ApiResponse.success("Student status updated successfully", studentResponse);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<java.util.List<com.lab.atlasmentor.model.StudentActivity>>> getStudentActivities(@PathVariable Long id) {
        try {
            java.util.List<com.lab.atlasmentor.model.StudentActivity> activities = studentService.getStudentActivities(id);
            ApiResponse<java.util.List<com.lab.atlasmentor.model.StudentActivity>> response = ApiResponse.success("Student activities retrieved successfully", activities);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<java.util.List<com.lab.atlasmentor.model.StudentActivity>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/required-documents")
    public ResponseEntity<ApiResponse<java.util.Map<String, java.util.List<String>>>> getRequiredDocuments() {
        try {
            java.util.Map<String, java.util.List<String>> requiredDocuments = studentService.getRequiredDocuments();
            ApiResponse<java.util.Map<String, java.util.List<String>>> response = ApiResponse.success("Required documents retrieved successfully", requiredDocuments);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<java.util.Map<String, java.util.List<String>>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/with-payment-by-referral-company")
    public ResponseEntity<ApiResponse<ClientPayoutWithSummaryDto>> getStudentsWithPaymentByReferralAndCompany(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long branch,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            ClientPayoutWithSummaryDto result = studentService.getStudentsWithPaymentByReferralAndCompanyWithSummary(
                search, source, branch, paymentStatus, dateFrom, dateTo, page, size);
            ApiResponse<ClientPayoutWithSummaryDto> response = ApiResponse.success("Client payouts with referral and company details retrieved successfully", result);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<ClientPayoutWithSummaryDto> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/payment/amount")
    public ResponseEntity<ApiResponse<StudentPayment>> updateStudentPaymentAmount(
            @Valid @RequestBody StudentPaymentAmountUpdateRequest request) {
        try {
            StudentPayment updatedPayment = studentService.updateStudentPaymentAmount(request);
            ApiResponse<StudentPayment> response = ApiResponse.success("Payment amount updated successfully", updatedPayment);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/client-payout/amount")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> updateClientPayoutAmount(
            @Valid @RequestBody ClientPayoutAmountUpdateRequest request) {
        try {
            ClientPayout updatedPayout = studentService.updateClientPayoutAmount(request);
            ClientPayoutDto payoutDto = studentService.convertToClientPayoutDto(updatedPayout);
            ApiResponse<ClientPayoutDto> response = ApiResponse.success("Client payout amount updated successfully", payoutDto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<ClientPayoutDto> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/payment/status")
    public ResponseEntity<ApiResponse<StudentPayment>> updateStudentPaymentStatus(
            @Valid @RequestBody StudentPaymentStatusUpdateRequest request) {
        try {
            StudentPayment updatedPayment = studentService.updateStudentPaymentStatus(request);
            ApiResponse<StudentPayment> response = ApiResponse.success("Payment status updated successfully", updatedPayment);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/payment/amount")
    public ResponseEntity<ApiResponse<StudentPaymentAmountDto>> getStudentPaymentAmount(@RequestParam Long studentId) {
        try {
            StudentPaymentAmountDto paymentAmount = studentService.getStudentPaymentAmount(studentId);
            ApiResponse<StudentPaymentAmountDto> response = ApiResponse.success("Payment amount retrieved successfully", paymentAmount);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<StudentPaymentAmountDto> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

