package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.model.Country;
import com.lab.atlasmentor.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Country>>> getAllCountries() {
        try {
            List<Country> countries = countryService.getAllActiveCountries();
            if (countries.isEmpty()) {
                ApiResponse<List<Country>> response = ApiResponse.success("No data found", countries);
                return ResponseEntity.ok(response);
            }
            ApiResponse<List<Country>> response = ApiResponse.success("Countries retrieved successfully", countries);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<Country>> response = ApiResponse.error("Failed to retrieve countries: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Country>>> getAllCountriesIncludingInactive() {
        try {
            List<Country> countries = countryService.getAllCountries();
            if (countries.isEmpty()) {
                ApiResponse<List<Country>> response = ApiResponse.success("No data found", countries);
                return ResponseEntity.ok(response);
            }
            ApiResponse<List<Country>> response = ApiResponse.success("All countries retrieved successfully", countries);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<Country>> response = ApiResponse.error("Failed to retrieve countries: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Country>> getCountryById(@PathVariable Long id) {
        try {
            return countryService.getCountryById(id)
                .map(country -> {
                    ApiResponse<Country> response = ApiResponse.success("Country retrieved successfully", country);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<Country> response = ApiResponse.error("Country not found with id: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
        } catch (BusinessException e) {
            ApiResponse<Country> response = ApiResponse.error("Failed to retrieve country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Country>> getCountryByCode(@PathVariable String code) {
        try {
            return countryService.getCountryByCode(code)
                .map(country -> {
                    ApiResponse<Country> response = ApiResponse.success("Country retrieved successfully", country);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<Country> response = ApiResponse.error("Country not found with code: " + code);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
        } catch (BusinessException e) {
            ApiResponse<Country> response = ApiResponse.error("Failed to retrieve country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Country>> createCountry(@RequestBody Country country) {
        try {
            if (countryService.existsByCode(country.getCode())) {
                ApiResponse<Country> response = ApiResponse.error("Country with code already exists: " + country.getCode());
                return ResponseEntity.badRequest().body(response);
            }
            if (countryService.existsByName(country.getName())) {
                ApiResponse<Country> response = ApiResponse.error("Country with name already exists: " + country.getName());
                return ResponseEntity.badRequest().body(response);
            }
            
            Country createdCountry = countryService.createCountry(country);
            ApiResponse<Country> response = ApiResponse.success("Country created successfully", createdCountry);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<Country> response = ApiResponse.error("Failed to create country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Country>> updateCountry(@PathVariable Long id, @RequestBody Country country) {
        try {
            Country updatedCountry = countryService.updateCountry(id, country);
            if (updatedCountry != null) {
                ApiResponse<Country> response = ApiResponse.success("Country updated successfully", updatedCountry);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<Country> response = ApiResponse.error("Country not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (BusinessException e) {
            ApiResponse<Country> response = ApiResponse.error("Failed to update country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCountry(@PathVariable Long id) {
        try {
            boolean deleted = countryService.deleteCountry(id);
            if (deleted) {
                ApiResponse<String> response = ApiResponse.success("Country deleted successfully", "Country with id " + id + " has been deleted");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("Country not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (BusinessException e) {
            ApiResponse<String> response = ApiResponse.error("Failed to delete country: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
