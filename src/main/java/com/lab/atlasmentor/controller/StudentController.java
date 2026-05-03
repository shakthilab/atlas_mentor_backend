package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.dto.StudentOnboardingRequest;
import com.lab.atlasmentor.dto.StudentStatusUpdateRequest;
import com.lab.atlasmentor.dto.StudentResponse;
import com.lab.atlasmentor.dto.StudentWithStudentPaymentDto;
import com.lab.atlasmentor.dto.StudentPaymentAmountUpdateRequest;
import com.lab.atlasmentor.dto.StudentPaymentStatusUpdateRequest;
import com.lab.atlasmentor.dto.StudentPaymentAmountDto;
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
    public ResponseEntity<ApiResponse<Student>> getStudent(@PathVariable Long id) {
        try {
            Student student = studentService.getStudentByIdAsResponse(id);
            ApiResponse<Student> response = ApiResponse.success("Student retrieved successfully", student);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<Student> response = ApiResponse.error(e.getMessage());
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
    public ResponseEntity<ApiResponse<PageResponse<Student>>> getAllStudents(
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<Student> students = studentService.getAllStudents(status, search, pageable);
        ApiResponse<PageResponse<Student>> response = ApiResponse.success("Students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registered")
    public ResponseEntity<ApiResponse<PageResponse<Student>>> getRegisteredStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<Student> students = studentService.getAllStudents(StudentStatus.REGISTERED, search, pageable);
        ApiResponse<PageResponse<Student>> response = ApiResponse.success("Registered students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/non-registered")
    public ResponseEntity<ApiResponse<PageResponse<Student>>> getNonRegisteredStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<Student> students = studentService.getNonRegisteredStudents(search, pageable);
        ApiResponse<PageResponse<Student>> response = ApiResponse.success("Non-registered students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByEmail(@PathVariable String email) {
        try {
            Student student = studentService.findStudentByEmail(email);
            if (student != null) {
                StudentResponse studentResponse = StudentResponse.fromEntity(student);
                ApiResponse<StudentResponse> response = ApiResponse.success("Student found successfully", studentResponse);
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
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
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
    public ResponseEntity<ApiResponse<java.util.List<StudentWithStudentPaymentDto>>> getStudentsWithPaymentByReferralAndCompany() {
        try {
            java.util.List<StudentWithStudentPaymentDto> students = studentService.getStudentsWithPaymentByReferralAndCompany();
            ApiResponse<java.util.List<StudentWithStudentPaymentDto>> response = ApiResponse.success("Students with payment details (referral and company) retrieved successfully", students);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<java.util.List<StudentWithStudentPaymentDto>> response = ApiResponse.error(e.getMessage());
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

