package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.ClientPayoutDto;
import com.lab.atlasmentor.dto.DisputeResponseRequest;
import com.lab.atlasmentor.dto.PaymentDisputeActivityDto;
import com.lab.atlasmentor.dto.PaymentDisputeRequest;
import com.lab.atlasmentor.dto.UserInfoDto;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.PaymentDisputeActivity;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.ClientPayoutRepository;
import com.lab.atlasmentor.repository.StudentPaymentRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.service.ClientPayoutService;
import com.lab.atlasmentor.service.PaymentDisputeActivityService;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentDisputeActivityController {

    @Autowired
    private PaymentDisputeActivityService activityService;

    @Autowired
    private ClientPayoutService clientPayoutService;

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Autowired
    private ClientPayoutRepository clientPayoutRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{paymentId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<PaymentDisputeActivityDto>>> getPaymentActivities(
            @PathVariable Long paymentId) {
        try {
            List<PaymentDisputeActivity> activities = activityService.getPaymentDisputeActivities(paymentId);
            List<PaymentDisputeActivityDto> activityDtos = activities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Activities retrieved successfully", activityDtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/dispute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> raiseDispute(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentDisputeRequest request) {
        try {
            Optional<StudentPayment> paymentOpt = studentPaymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("Payment not found"));
            }

            StudentPayment payment = paymentOpt.get();
            
            // Find the corresponding ClientPayout for this student and source
            List<ClientPayout> clientPayouts = clientPayoutRepository.findByStudentIdAndSourceType(
                payment.getStudent().getId(), payment.getSourceType());
            
            if (clientPayouts.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("No client payout found for this payment"));
            }
            
            // Use the first matching client payout (or implement logic to select the right one)
            ClientPayout clientPayout = clientPayouts.get(0);

            ClientPayout updatedPayout = clientPayoutService.initiateDispute(
                    clientPayout.getId(), request.getDisputeReason());

            ClientPayoutDto payoutDto = convertToDto(updatedPayout);
            return ResponseEntity.ok(ApiResponse.success("Dispute raised successfully", payoutDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/payouts/{payoutId}/dispute/accept")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> acceptDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody DisputeResponseRequest request) {
        try {
            ClientPayout updatedPayout = clientPayoutService.acceptDispute(payoutId, request.getResponse());
            ClientPayoutDto payoutDto = convertToDto(updatedPayout);
            return ResponseEntity.ok(ApiResponse.success("Dispute accepted successfully", payoutDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/payouts/{payoutId}/dispute/reject")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<ClientPayoutDto>> rejectDispute(
            @PathVariable Long payoutId,
            @Valid @RequestBody DisputeResponseRequest request) {
        try {
            ClientPayout updatedPayout = clientPayoutService.rejectDispute(payoutId, request.getResponse());
            ClientPayoutDto payoutDto = convertToDto(updatedPayout);
            return ResponseEntity.ok(ApiResponse.success("Dispute rejected successfully", payoutDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    private PaymentDisputeActivityDto convertToDto(PaymentDisputeActivity activity) {
        UserInfoDto doneByDto = new UserInfoDto();
        doneByDto.setId(activity.getDoneBy().getId());
        doneByDto.setUsername(activity.getDoneBy().getFullName());
        doneByDto.setEmail(activity.getDoneBy().getEmail());
        doneByDto.setRole(activity.getDoneBy().getRole().getName());

        PaymentDisputeActivityDto dto = new PaymentDisputeActivityDto(
            activity.getId(),
            activity.getPayment().getId(),
            activity.getAction(),
            activity.getOldValue(),
            activity.getNewValue(),
            activity.getReason(),
            doneByDto,
            activity.getDoneAt()
        );
        dto.setStatus(activity.getStatus());
        dto.setUpdatedAt(activity.getUpdatedAt());
        return dto;
    }

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
        
        return dto;
    }

    private UserInfoDto convertToUserInfoDto(User user) {
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        return dto;
    }
}
