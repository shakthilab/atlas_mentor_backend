package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.PaymentTransactionDto;
import com.lab.atlasmentor.dto.PaymentTransactionRequest;
import com.lab.atlasmentor.dto.AmountChangeRequest;
import com.lab.atlasmentor.dto.StatusChangeRequest;
import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.service.FinalPaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EnhancedPaymentController {

    @Autowired
    private FinalPaymentService finalPaymentService;

    // ==================== STUDENT PAYMENT MANAGEMENT ====================

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<StudentPayment> createStudentPayment(@RequestParam Long studentId, 
                                                             @RequestParam String sourceType, 
                                                             @RequestParam Long sourceId) {
        try {
            StudentPayment payment = finalPaymentService.createStudentPayment(studentId, 
                com.lab.atlasmentor.enums.SourceType.valueOf(sourceType), sourceId);
            return ResponseEntity.ok(payment);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{studentId}/assign-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<StudentPayment> assignAmount(@PathVariable Long studentId, 
                                                     @RequestParam BigDecimal amount) {
        try {
            StudentPayment payment = finalPaymentService.assignAmount(studentId, amount);
            return ResponseEntity.ok(payment);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ==================== PAYMENT TRANSACTIONS ====================

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<ApiResponse<PaymentTransaction>> addPaymentTransaction(@Valid @RequestBody PaymentTransactionRequest request) {
        try {
            PaymentTransaction transaction = finalPaymentService.addPaymentTransaction(
                request.getStudentId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getTransactionType(),
                request.getTransactionReference(),
                request.getNotes()
            );
            return ResponseEntity.ok(ApiResponse.success("Payment transaction created successfully", transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{studentId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<ApiResponse<List<PaymentTransactionDto>>> getPaymentTransactions(@PathVariable Long studentId) {
        try {
            List<PaymentTransactionDto> transactions = finalPaymentService.getPaymentTransactions(studentId);
            return ResponseEntity.ok(ApiResponse.success("Payment transactions retrieved successfully", transactions));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{studentId}/net-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<BigDecimal> getNetPaidAmount(@PathVariable Long studentId) {
        try {
            BigDecimal amount = finalPaymentService.getNetPaidAmount(studentId);
            return ResponseEntity.ok(amount);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== AMOUNT CHANGE WORKFLOW ====================

    @PostMapping("/amount-change/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<PaymentAmountChange> requestAmountChange(@Valid @RequestBody AmountChangeRequest request) {
        try {
            PaymentAmountChange amountChange = finalPaymentService.requestAmountChange(
                request.getStudentId(),
                request.getNewAmount(),
                request.getRemarks()
            );
            return ResponseEntity.ok(amountChange);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/amount-change/approve/{changeRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<PaymentAmountChange> approveAmountChange(@PathVariable Long changeRequestId,
                                                                  @RequestParam String approvalRemarks) {
        try {
            PaymentAmountChange amountChange = finalPaymentService.approveAmountChange(changeRequestId, approvalRemarks);
            return ResponseEntity.ok(amountChange);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== STATUS CHANGE WORKFLOW ====================

    @PostMapping("/status-change/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT')")
    public ResponseEntity<StudentStatusApproval> requestStatusChange(@Valid @RequestBody StatusChangeRequest request) {
        try {
            StudentStatusApproval statusApproval = finalPaymentService.requestStatusChange(
                request.getStudentId(),
                request.getNewStatus(),
                request.getReason(),
                request.getProofUrl()
            );
            return ResponseEntity.ok(statusApproval);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/status-change/approve/{approvalRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<StudentStatusApproval> approveStatusChange(@PathVariable Long approvalRequestId,
                                                                   @RequestParam String approvalRemarks) {
        try {
            StudentStatusApproval statusApproval = finalPaymentService.approveStatusChange(approvalRequestId, approvalRemarks);
            return ResponseEntity.ok(statusApproval);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== PAYMENT QUERY METHODS ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<List<StudentPayment>> getStudentPaymentsByRole() {
        try {
            List<StudentPayment> payments = finalPaymentService.getStudentPaymentsByRole();
            return ResponseEntity.ok(payments);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== REPORTING ====================

    @GetMapping("/reports/{sourceId}/{sourceType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE_ASSISTANT', 'REFERRAL', 'COMPANY')")
    public ResponseEntity<FinalPaymentService.ReportingData> getReportingData(@PathVariable Long sourceId,
                                                                              @PathVariable String sourceType) {
        try {
            FinalPaymentService.ReportingData data = finalPaymentService.getReportingData(
                sourceId, 
                com.lab.atlasmentor.enums.SourceType.valueOf(sourceType)
            );
            return ResponseEntity.ok(data);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
