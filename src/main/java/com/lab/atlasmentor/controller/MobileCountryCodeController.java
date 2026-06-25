package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.MobileCountryCodeResponse;
import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.service.MobileCountryCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile-country-codes")
@CrossOrigin(origins = "*")
public class MobileCountryCodeController {

    @Autowired
    private MobileCountryCodeService mobileCountryCodeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MobileCountryCodeResponse>>> getAllMobileCountryCodes() {
        try {
            List<MobileCountryCode> countryCodes = mobileCountryCodeService.getAllMobileCountryCodes();
            List<MobileCountryCodeResponse> response = countryCodes.stream()
                    .map(MobileCountryCodeResponse::fromEntity)
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", response));
            }
            return ResponseEntity.ok(ApiResponse.success("Mobile country codes retrieved successfully", response));
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve mobile country codes"));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<MobileCountryCodeResponse>>> getActiveMobileCountryCodes() {
        try {
            List<MobileCountryCode> countryCodes = mobileCountryCodeService.getActiveMobileCountryCodes();
            List<MobileCountryCodeResponse> response = countryCodes.stream()
                    .map(MobileCountryCodeResponse::fromEntity)
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", response));
            }
            return ResponseEntity.ok(ApiResponse.success("Active mobile country codes retrieved successfully", response));
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve active mobile country codes"));
        }
    }

    @GetMapping("/iso-alpha-2/{isoAlpha2}")
    public ResponseEntity<ApiResponse<MobileCountryCodeResponse>> getByIsoAlpha2(@PathVariable String isoAlpha2) {
        try {
            Optional<MobileCountryCode> countryCode = mobileCountryCodeService.getByIsoAlpha2(isoAlpha2.toUpperCase());

            if (countryCode.isPresent()) {
                MobileCountryCodeResponse response = MobileCountryCodeResponse.fromEntity(countryCode.get());
                return ResponseEntity.ok(ApiResponse.success("Country code found", response));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve country code"));
        }
    }

    @GetMapping("/iso-alpha-3/{isoAlpha3}")
    public ResponseEntity<ApiResponse<MobileCountryCodeResponse>> getByIsoAlpha3(@PathVariable String isoAlpha3) {
        try {
            Optional<MobileCountryCode> countryCode = mobileCountryCodeService.getByIsoAlpha3(isoAlpha3.toUpperCase());

            if (countryCode.isPresent()) {
                MobileCountryCodeResponse response = MobileCountryCodeResponse.fromEntity(countryCode.get());
                return ResponseEntity.ok(ApiResponse.success("Country code found", response));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve country code"));
        }
    }

    @GetMapping("/mobile-code/{mobileCode}")
    public ResponseEntity<ApiResponse<List<MobileCountryCodeResponse>>> getByMobileCode(@PathVariable String mobileCode) {
        try {
            List<MobileCountryCode> countryCodes = mobileCountryCodeService.getByMobileCode(mobileCode);
            List<MobileCountryCodeResponse> response = countryCodes.stream()
                    .map(MobileCountryCodeResponse::fromEntity)
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", response));
            }
            return ResponseEntity.ok(ApiResponse.success("Countries found for mobile code", response));
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve countries by mobile code"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MobileCountryCodeResponse>>> searchByCountryName(
            @RequestParam String countryName) {
        try {
            List<MobileCountryCode> countryCodes = mobileCountryCodeService.searchByCountryName(countryName);
            List<MobileCountryCodeResponse> response = countryCodes.stream()
                    .map(MobileCountryCodeResponse::fromEntity)
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No data found", response));
            }
            return ResponseEntity.ok(ApiResponse.success("Countries found matching search criteria", response));
        } catch (BusinessException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search countries"));
        }
    }
}
