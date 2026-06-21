package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.ClientPayoutDto;
import com.lab.atlasmentor.dto.ClientPayoutActivityDto;
import com.lab.atlasmentor.dto.AmountAssignmentRequest;
import com.lab.atlasmentor.dto.PaymentRequest;
import com.lab.atlasmentor.dto.DisputeRequest;
import com.lab.atlasmentor.dto.PaymentDisputeRequest;
import com.lab.atlasmentor.dto.DisputeResponseRequest;
import com.lab.atlasmentor.dto.UserInfoDto;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.ClientPayoutActivity;
import com.lab.atlasmentor.service.ClientPayoutService;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/client-payouts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ClientPayoutController {

    @Autowired
    private ClientPayoutService clientPayoutService;

    // ==================== VIEW PAYOUTS ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<ClientPayoutDto>>> getClientPayouts() {
        try {
            List<ClientPayout> payouts = clientPayoutService.getClientPayoutsByRole();
            List<ClientPayoutDto> payoutDtos = payouts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Client payouts retrieved successfully", payoutDtos));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/{payoutId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> getClientPayoutById(@PathVariable Long payoutId) {
        try {
            ClientPayout payout = clientPayoutService.getClientPayoutById(payoutId);
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Client payout retrieved successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/{payoutId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<ClientPayoutActivityDto>>> getPayoutActivities(@PathVariable Long payoutId) {
        try {
            List<ClientPayoutActivity> activities = clientPayoutService.getPayoutActivities(payoutId);
            List<ClientPayoutActivityDto> activityDtos = activities.stream()
                .map(this::convertActivityToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Payout activities retrieved successfully", activityDtos));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/{payoutId}/dispute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> initiateDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody PaymentDisputeRequest request) {
        try {
            ClientPayout updatedPayout = clientPayoutService.initiateDispute(payoutId, request.getDisputeReason());
            ClientPayoutDto payoutDto = convertToDto(updatedPayout);
            return ResponseEntity.ok(ApiResponse.success("Dispute initiated successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== AMOUNT ASSIGNMENT ====================

    @PostMapping("/{payoutId}/assign-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> assignAmount(
            @PathVariable Long payoutId,
            @Valid @RequestBody AmountAssignmentRequest request) {
        try {
            ClientPayout payout = clientPayoutService.assignAmount(payoutId, request.getAmount(), request.getNotes());
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Amount assigned successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== PAYMENT PROCESSING ====================

    @PostMapping("/{payoutId}/add-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> addPayment(
            @PathVariable Long payoutId,
            @Valid @RequestBody PaymentRequest request) {
        try {
            ClientPayout payout = clientPayoutService.addPayment(
                payoutId, 
                request.getAmount(), 
                request.getPaymentMethod(), 
                request.getTransactionReference(), 
                request.getNotes()
            );
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Payment added successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== DISPUTE MANAGEMENT ====================

    @PostMapping("/{payoutId}/initiate-dispute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> initiateDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody DisputeRequest request) {
        try {
            ClientPayout payout = clientPayoutService.initiateDispute(payoutId, request.getReason());
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Dispute initiated successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/{payoutId}/accept-dispute")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> acceptDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody DisputeResponseRequest request) {
        try {
            ClientPayout payout = clientPayoutService.acceptDispute(payoutId, request.getResponse());
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Dispute accepted successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/{payoutId}/reject-dispute")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> rejectDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody DisputeResponseRequest request) {
        try {
            ClientPayout payout = clientPayoutService.rejectDispute(payoutId, request.getResponse());
            ClientPayoutDto payoutDto = convertToDto(payout);
            
            return ResponseEntity.ok(ApiResponse.success("Dispute rejected successfully", payoutDto));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== REPORTING ====================

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_PARTNER')")
    public ResponseEntity<ApiResponse<Object>> getPayoutSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType) {
        try {
            // Implementation for summary report
            return ResponseEntity.ok(ApiResponse.success("Summary report retrieved successfully", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private ClientPayoutDto convertToDto(ClientPayout payout) {
        ClientPayoutDto dto = new ClientPayoutDto();
        dto.setId(payout.getId());
        dto.setStudentId(payout.getStudentId());
        dto.setStudentName(payout.getStudent() != null && payout.getStudent().getUser() != null ? 
        payout.getStudent().getUser().getFirstName() + " " + payout.getStudent().getUser().getLastName() : null);
        dto.setSourceType(payout.getSourceType());
        dto.setAssignedAmount(payout.getAssignedAmount());
        dto.setPaidAmount(payout.getPaidAmount());
        dto.setBalanceAmount(payout.getBalanceAmount());
        dto.setSettledAmount(payout.getSettledAmount());
        dto.setPayoutStatus(payout.getPayoutStatus());
        dto.setPreviousStatus(payout.getPreviousStatus());
        dto.setPaymentProgress(payout.getPaymentProgress());
        dto.setPaymentStageDisplay(payout.getPaymentStageDisplay());
        
        // Dispute tracking
        dto.setDisputeReason(payout.getDisputeReason());
        dto.setDisputeResponse(payout.getDisputeResponse());
        dto.setDisputeAmount(payout.getDisputeAmount());
        dto.setDisputedAt(payout.getDisputedAt());
        dto.setRespondedAt(payout.getRespondedAt());
        
        // User tracking
        if (payout.getUser() != null) {
            dto.setUser(convertToUserInfoDto(payout.getUser()));
        }
        if (payout.getAssignedBy() != null) {
            dto.setAssignedBy(convertToUserInfoDto(payout.getAssignedBy()));
        }
        if (payout.getDisputedBy() != null) {
            dto.setDisputedBy(convertToUserInfoDto(payout.getDisputedBy()));
        }
        if (payout.getRespondedBy() != null) {
            dto.setRespondedBy(convertToUserInfoDto(payout.getRespondedBy()));
        }
        if (payout.getLastPaidBy() != null) {
            dto.setLastPaidBy(convertToUserInfoDto(payout.getLastPaidBy()));
        }
        
        // Timeline tracking
        dto.setAssignedAt(payout.getAssignedAt());
        dto.setLastPaidAt(payout.getLastPaidAt());
        dto.setPaymentMethod(payout.getPaymentMethod());
        dto.setTransactionReference(payout.getTransactionReference());
        dto.setNotes(payout.getNotes());
        dto.setCreatedAt(payout.getCreatedAt());
        dto.setUpdatedAt(payout.getUpdatedAt());
        
        return dto;
    }

    private ClientPayoutActivityDto convertActivityToDto(ClientPayoutActivity activity) {
        UserInfoDto doneByDto = null;
        if (activity.getDoneBy() != null) {
            doneByDto = convertToUserInfoDto(activity.getDoneBy());
        }

        ClientPayoutActivityDto dto = new ClientPayoutActivityDto(
            activity.getId(),
            activity.getClientPayout() != null ? activity.getClientPayout().getId() : null,
            activity.getAction(),
            activity.getOldValue(),
            activity.getNewValue(),
            activity.getReason(),
            doneByDto,
            activity.getDoneAt()
        );
        
        dto.setOldAmount(activity.getOldAmount());
        dto.setNewAmount(activity.getNewAmount());
        dto.setPaymentMethod(activity.getPaymentMethod());
        dto.setTransactionReference(activity.getTransactionReference());
        dto.setDisputeStage(activity.getDisputeStage());
        dto.setPreviousStatus(activity.getPreviousStatus());
        dto.setNewStatus(activity.getNewStatus());
        
        return dto;
    }

    private UserInfoDto convertToUserInfoDto(com.lab.atlasmentor.model.User user) {
        if (user == null) return null;
        
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getFirstName() + " " + user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().getName() : null);
        
        return dto;
    }
}
