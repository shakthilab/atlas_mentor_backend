package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.model.University;
import com.lab.atlasmentor.service.UniversityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    @Autowired
    private UniversityService universityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<University>>> getAllUniversities() {
        try {
            List<University> universities = universityService.getAllActiveUniversities();
            if (universities.isEmpty()) {
                ApiResponse<List<University>> response = ApiResponse.success("No data found", universities);
                return ResponseEntity.ok(response);
            }
            ApiResponse<List<University>> response = ApiResponse.success("Universities retrieved successfully", universities);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<University>> response = ApiResponse.error("Failed to retrieve universities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<University>>> getAllUniversitiesIncludingInactive() {
        try {
            List<University> universities = universityService.getAllUniversities();
            if (universities.isEmpty()) {
                ApiResponse<List<University>> response = ApiResponse.success("No data found", universities);
                return ResponseEntity.ok(response);
            }
            ApiResponse<List<University>> response = ApiResponse.success("All universities retrieved successfully", universities);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<University>> response = ApiResponse.error("Failed to retrieve universities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<University>> getUniversityById(@PathVariable Long id) {
        try {
            return universityService.getUniversityById(id)
                .map(university -> {
                    ApiResponse<University> response = ApiResponse.success("University retrieved successfully", university);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<University> response = ApiResponse.error("University not found with id: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
        } catch (BusinessException e) {
            ApiResponse<University> response = ApiResponse.error("Failed to retrieve university: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<University>>> getUniversitiesByCountryId(@PathVariable Long countryId) {
        try {
            List<University> universities = universityService.getActiveUniversitiesByCountryId(countryId);
            if (universities.isEmpty()) {
                ApiResponse<List<University>> response = ApiResponse.success("No data found", universities);
                return ResponseEntity.ok(response);
            }
            ApiResponse<List<University>> response = ApiResponse.success("Universities retrieved successfully for country: " + countryId, universities);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<University>> response = ApiResponse.error("Failed to retrieve universities for country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<University>> createUniversity(@RequestBody University university) {
        try {
            if (universityService.existsByNameAndCountryId(university.getName(), university.getCountryId())) {
                ApiResponse<University> response = ApiResponse.error("University with this name already exists in the specified country");
                return ResponseEntity.badRequest().body(response);
            }

            University createdUniversity = universityService.createUniversity(university);
            ApiResponse<University> response = ApiResponse.success("University created successfully", createdUniversity);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<University> response = ApiResponse.error("Failed to create university: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/simple")
    public ResponseEntity<ApiResponse<University>> createUniversitySimple(@RequestBody UniversitySimpleRequest request) {
        try {
            if (universityService.existsByNameAndCountryId(request.getName(), request.getCountryId())) {
                ApiResponse<University> response = ApiResponse.error("University with this name already exists in the specified country");
                return ResponseEntity.badRequest().body(response);
            }

            University createdUniversity = universityService.createUniversity(request.getName(), request.getCountryId());
            if (createdUniversity != null) {
                ApiResponse<University> response = ApiResponse.success("University created successfully", createdUniversity);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                ApiResponse<University> response = ApiResponse.error("Country not found with id: " + request.getCountryId());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (BusinessException e) {
            ApiResponse<University> response = ApiResponse.error("Failed to create university: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<University>> updateUniversity(@PathVariable Long id, @RequestBody University university) {
        try {
            University updatedUniversity = universityService.updateUniversity(id, university);
            if (updatedUniversity != null) {
                ApiResponse<University> response = ApiResponse.success("University updated successfully", updatedUniversity);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<University> response = ApiResponse.error("University not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (BusinessException e) {
            ApiResponse<University> response = ApiResponse.error("Failed to update university: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUniversity(@PathVariable Long id) {
        try {
            boolean deleted = universityService.deleteUniversity(id);
            if (deleted) {
                ApiResponse<String> response = ApiResponse.success("University deleted successfully", "University with id " + id + " has been deleted");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("University not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (BusinessException e) {
            ApiResponse<String> response = ApiResponse.error("Failed to delete university: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public static class UniversitySimpleRequest {
        private String name;
        private Long countryId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getCountryId() {
            return countryId;
        }

        public void setCountryId(Long countryId) {
            this.countryId = countryId;
        }
    }
}
