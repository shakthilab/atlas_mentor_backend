package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.DisputeDto;
import com.lab.atlasmentor.dto.DisputeRequest;
import com.lab.atlasmentor.dto.DisputeResolutionRequest;
import com.lab.atlasmentor.dto.DisputeAcceptRequest;
import com.lab.atlasmentor.dto.DisputeRejectRequest;
import com.lab.atlasmentor.model.Dispute;
import com.lab.atlasmentor.service.DisputeService;
import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.DisputePriority;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    // ==================== DISPUTE CRUD OPERATIONS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> createDispute(@Valid @RequestBody DisputeRequest request) {
        try {
            DisputeDto dispute = disputeService.createDispute(request);
            return ResponseEntity.ok(ApiResponse.success("Dispute created successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<DisputeDto>> getDisputeById(@PathVariable Long id) {
        try {
            DisputeDto dispute = disputeService.getDisputeById(id);
            return ResponseEntity.ok(ApiResponse.success("Dispute retrieved successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<DisputeDto>>> getAllDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputePriority priority,
            @RequestParam(required = false) Long studentId) {
        try {
            List<DisputeDto> disputes = disputeService.getAllDisputes(status, priority, studentId);
            return ResponseEntity.ok(ApiResponse.success("Disputes retrieved successfully", disputes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-disputes")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<DisputeDto>>> getMyDisputes(
            @RequestParam(required = false) DisputeStatus status) {
        try {
            List<DisputeDto> disputes = disputeService.getMyDisputes(status);
            return ResponseEntity.ok(ApiResponse.success("My disputes retrieved successfully", disputes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> updateDisputeStatus(@PathVariable Long id,
                                                                      @RequestParam DisputeStatus status) {
        try {
            DisputeDto dispute = disputeService.updateDisputeStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Dispute status updated successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> resolveDispute(@PathVariable Long id,
                                                                  @Valid @RequestBody DisputeResolutionRequest request) {
        try {
            DisputeDto dispute = disputeService.resolveDispute(id, request.getResolutionNotes(), request.getResolutionStatus());
            return ResponseEntity.ok(ApiResponse.success("Dispute resolved successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> acceptDispute(@PathVariable Long id,
                                                                @Valid @RequestBody DisputeAcceptRequest request) {
        try {
            DisputeDto dispute = disputeService.acceptDispute(id, request.getAcceptanceNotes());
            return ResponseEntity.ok(ApiResponse.success("Dispute accepted successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> rejectDispute(@PathVariable Long id,
                                                                @Valid @RequestBody DisputeRejectRequest request) {
        try {
            DisputeDto dispute = disputeService.rejectDispute(id, request.getRejectionReason());
            return ResponseEntity.ok(ApiResponse.success("Dispute rejected successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DisputeDto>> closeDispute(@PathVariable Long id,
                                                                @RequestParam String resolutionNotes) {
        try {
            DisputeDto dispute = disputeService.closeDispute(id, resolutionNotes);
            return ResponseEntity.ok(ApiResponse.success("Dispute closed successfully", dispute));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteDispute(@PathVariable Long id) {
        try {
            disputeService.deleteDispute(id);
            return ResponseEntity.ok(ApiResponse.success("Dispute deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
