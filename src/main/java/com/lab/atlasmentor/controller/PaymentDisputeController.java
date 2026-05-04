package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.PaymentDisputeRequest;
import com.lab.atlasmentor.dto.PaymentDisputeResponseRequest;
import com.lab.atlasmentor.dto.StudentPaymentDto;
import com.lab.atlasmentor.service.PaymentDisputeService;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentDisputeController {

    @Autowired
    private PaymentDisputeService paymentDisputeService;

    // Admin marks payment as disputed
    @PostMapping("/{paymentId}/dispute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<StudentPaymentDto>> markPaymentAsDisputed(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentDisputeRequest request) {
        try {
            CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
            StudentPaymentDto payment = paymentDisputeService.markPaymentAsDisputed(
                paymentId, request.getDisputeReason(), currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Payment marked as disputed successfully", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // REFERRAL/COMPANY accepts dispute (sets status to REJECTED)
    @PostMapping("/{paymentId}/dispute/accept")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<StudentPaymentDto>> acceptPaymentDispute(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentDisputeResponseRequest request) {
        try {
            CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
            StudentPaymentDto payment = paymentDisputeService.acceptPaymentDispute(
                paymentId, request.getResponse(), currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Dispute accepted successfully", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // REFERRAL/COMPANY rejects dispute (keeps DISPUTE status with comments)
    @PostMapping("/{paymentId}/dispute/reject")
    @PreAuthorize("hasAnyRole('REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<StudentPaymentDto>> rejectPaymentDispute(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentDisputeResponseRequest request) {
        try {
            CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
            StudentPaymentDto payment = paymentDisputeService.rejectPaymentDispute(
                paymentId, request.getResponse(), currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Dispute rejected successfully", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}
