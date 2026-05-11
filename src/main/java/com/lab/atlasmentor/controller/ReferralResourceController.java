package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.ReferralResourceRequest;
import com.lab.atlasmentor.dto.ReferralResourceResponse;
import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import com.lab.atlasmentor.service.ReferralResourceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referral-resources")
public class ReferralResourceController {

    @Autowired
    private ReferralResourceService referralResourceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<List<ReferralResourceResponse>> createResource(
            @Valid @RequestBody ReferralResourceRequest request) {
        
        List<ReferralResourceResponse> responses = referralResourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<ReferralResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ReferralResourceRequest request) {
        
        ReferralResourceResponse response = referralResourceService.updateResource(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        
        referralResourceService.deleteResource(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ReferralResourceResponse> getResourceById(@PathVariable Long id) {
        
        ReferralResourceResponse response = referralResourceService.getResourceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<List<ReferralResourceResponse>> getResourcesByOwnerId(
            @PathVariable Long ownerId,
            @RequestParam OwnerType ownerType) {
        
        List<ReferralResourceResponse> responses = referralResourceService.getResourcesByOwnerId(ownerId, ownerType);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<PageResponse<ReferralResourceResponse>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) OwnerType ownerType,
            @RequestParam(required = false) String resourceType) {
        
        PageResponse<ReferralResourceResponse> responses = referralResourceService.getAllResources(
                page, size, ownerId, ownerType, resourceType);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-resources")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<List<ReferralResourceResponse>> getMyResources() {
        
        List<ReferralResourceResponse> responses = referralResourceService.getMyResources();
        return ResponseEntity.ok(responses);
    }
}
