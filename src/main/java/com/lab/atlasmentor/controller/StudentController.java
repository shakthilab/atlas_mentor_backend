package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
