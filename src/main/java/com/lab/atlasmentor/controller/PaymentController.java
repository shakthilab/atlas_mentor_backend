package com.lab.atlasmentor.controller;
import com.lab.atlasmentor.exception.BusinessException;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.PaymentCreateRequest;
import com.lab.atlasmentor.dto.PaymentUpdateRequest;
import com.lab.atlasmentor.model.ClientPayment;
import com.lab.atlasmentor.model.PaymentTransaction;
import com.lab.atlasmentor.service.PaymentService;
import com.lab.atlasmentor.enums.PaymentStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientPayment>> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        try {
            ClientPayment payment = paymentService.createPayment(request);
            ApiResponse<ClientPayment> response = ApiResponse.success("Payment created successfully", payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            ApiResponse<ClientPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ClientPayment>> updatePayment(@PathVariable Long paymentId,
                                                                    @Valid @RequestBody PaymentUpdateRequest request) {
        try {
            ClientPayment payment = paymentService.updatePaymentStatus(paymentId, request);
            ApiResponse<ClientPayment> response = ApiResponse.success("Payment updated successfully", payment);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<ClientPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{paymentId}/approve")
    public ResponseEntity<ApiResponse<ClientPayment>> approvePayment(@PathVariable Long paymentId) {
        try {
            PaymentUpdateRequest request = new PaymentUpdateRequest();
            request.setStatus(PaymentStatus.PAID);
            ClientPayment payment = paymentService.updatePaymentStatus(paymentId, request);
            ApiResponse<ClientPayment> response = ApiResponse.success("Payment approved successfully", payment);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<ClientPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{paymentId}/reject")
    public ResponseEntity<ApiResponse<ClientPayment>> rejectPayment(@PathVariable Long paymentId,
                                                                    @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String rejectionReason = requestBody != null ? requestBody.get("reason") : null;
            ClientPayment payment = paymentService.rejectPayment(paymentId, rejectionReason);
            ApiResponse<ClientPayment> response = ApiResponse.success("Payment rejected successfully", payment);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<ClientPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ClientPayment>> getPayment(@PathVariable Long paymentId) {
        try {
            ClientPayment payment = paymentService.getPaymentById(paymentId);
            ApiResponse<ClientPayment> response = ApiResponse.success("Payment retrieved successfully", payment);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<ClientPayment> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<ClientPayment>>> getPaymentsByStudent(@PathVariable Long studentId) {
        try {
            List<ClientPayment> payments = paymentService.getPaymentsByStudent(studentId);
            ApiResponse<List<ClientPayment>> response = ApiResponse.success("Payments retrieved successfully", payments);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ClientPayment>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ClientPayment>>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        try {
            List<ClientPayment> payments = paymentService.getPaymentsByStatus(status);
            ApiResponse<List<ClientPayment>> response = ApiResponse.success("Payments retrieved successfully", payments);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ClientPayment>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClientPayment>>> getPendingPayments() {
        try {
            List<ClientPayment> payments = paymentService.getPaymentsByStatus(PaymentStatus.PENDING);
            ApiResponse<List<ClientPayment>> response = ApiResponse.success("Pending payments retrieved successfully", payments);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ClientPayment>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/paid")
    public ResponseEntity<ApiResponse<List<ClientPayment>>> getPaidPayments() {
        try {
            List<ClientPayment> payments = paymentService.getPaymentsByStatus(PaymentStatus.PAID);
            ApiResponse<List<ClientPayment>> response = ApiResponse.success("Paid payments retrieved successfully", payments);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ClientPayment>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/rejected")
    public ResponseEntity<ApiResponse<List<ClientPayment>>> getRejectedPayments() {
        try {
            List<ClientPayment> payments = paymentService.getPaymentsByStatus(PaymentStatus.REJECTED);
            ApiResponse<List<ClientPayment>> response = ApiResponse.success("Rejected payments retrieved successfully", payments);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            ApiResponse<List<ClientPayment>> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}