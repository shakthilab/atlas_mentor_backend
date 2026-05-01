package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.model.Student;
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
            Student student = studentService.getStudentById(id);
            ApiResponse<Student> response = ApiResponse.success("Student retrieved successfully", student);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<Student> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Student>>> getAllStudents(
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Student> students = studentService.getAllStudents(status, search, pageable);
        ApiResponse<Page<Student>> response = ApiResponse.success("Students retrieved successfully", students);
        return ResponseEntity.ok(response);
    }
}

