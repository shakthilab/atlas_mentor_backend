package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.PaymentCreateRequest;
import com.lab.atlasmentor.dto.PaymentUpdateRequest;
import com.lab.atlasmentor.model.ClientPayment;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.PaymentTransaction;
import com.lab.atlasmentor.repository.ClientPaymentRepository;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.PaymentTransactionRepository;
import com.lab.atlasmentor.enums.PaymentStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private ClientPaymentRepository paymentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    
    @Transactional
    public ClientPayment createPayment(PaymentCreateRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can create payments
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can create payments.");
        }

        // Validate student exists
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + request.getStudentId()));

        // Check if payment already exists for this student
        Optional<ClientPayment> existingPayment = paymentRepository.findByStudentId(request.getStudentId());
        if (existingPayment.isPresent()) {
            throw new BusinessException("Payment already exists for student: " + request.getStudentId());
        }

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(student.getBranch() != null ? student.getBranch().getId() : null);
        }

        ClientPayment payment = new ClientPayment();
        payment.setStudentId(request.getStudentId());
        payment.setReferralId(request.getReferralId());
        payment.setCompanyId(request.getCompanyId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setBranchId(request.getBranchId());
        payment.setCreatedBy(currentUserDetails.getUserId());

        return paymentRepository.save(payment);
    }

    @Transactional
    public ClientPayment updatePaymentStatus(Long paymentId, PaymentUpdateRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can update payments
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can update payments.");
        }

        ClientPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            Student student = studentRepository.findById(payment.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found with id: " + payment.getStudentId()));
            validateManagerBranchAccess(student.getBranch() != null ? student.getBranch().getId() : null);
        }

        // Update payment details
        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
        }
        
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
            
            // Set approvedBy when status is changed to PAID
            if (PaymentStatus.PAID.equals(request.getStatus())) {
                payment.setApprovedBy(currentUserDetails.getUserId());
            }
        }

        payment.setUpdatedBy(currentUserDetails.getUserId());

        return paymentRepository.save(payment);
    }

    @Transactional
    public ClientPayment rejectPayment(Long paymentId, String rejectionReason) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can reject payments
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can reject payments.");
        }

        ClientPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            Student student = studentRepository.findById(payment.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found with id: " + payment.getStudentId()));
            validateManagerBranchAccess(student.getBranch() != null ? student.getBranch().getId() : null);
        }

        // Only pending payments can be rejected
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new BusinessException("Only pending payments can be rejected");
        }

        payment.setStatus(PaymentStatus.REJECTED);
        payment.setUpdatedBy(currentUserDetails.getUserId());

        return paymentRepository.save(payment);
    }

    public List<ClientPayment> getPaymentsByStudent(Long studentId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access based on role
        if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Referral can only see payments where they are the referral
            return paymentRepository.findByReferralIdOrderByCreatedAtDesc(currentUserDetails.getUserId());
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Company can only see payments where they are the company
            return paymentRepository.findByCompanyIdOrderByCreatedAtDesc(currentUserDetails.getUserId());
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            // Manager can see payments from their branch
            return paymentRepository.findByBranchIdOrderByCreatedAtDesc(currentUserDetails.getBranchId());
        } else if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin can see all payments
            return paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        } else {
            throw new BusinessException("Access denied");
        }
    }

    public List<ClientPayment> getPaymentsByStatus(PaymentStatus status) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER can view payments by status.");
        }

        if ("MANAGER".equalsIgnoreCase(userRole)) {
            return paymentRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(currentUserDetails.getBranchId(), status);
        } else {
            return paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        }
    }

    public ClientPayment getPaymentById(Long paymentId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        ClientPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Validate access based on role
        if ("REFERRAL".equalsIgnoreCase(userRole)) {
            if (!currentUserDetails.getUserId().equals(payment.getReferralId())) {
                throw new BusinessException("Access denied. You can only view your own payments.");
            }
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            if (!currentUserDetails.getUserId().equals(payment.getCompanyId())) {
                throw new BusinessException("Access denied. You can only view your own payments.");
            }
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            Student student = studentRepository.findById(payment.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found with id: " + payment.getStudentId()));
            validateManagerBranchAccess(student.getBranch() != null ? student.getBranch().getId() : null);
        }
        // Admin can view all payments

        return payment;
    }

    private void validateManagerBranchAccess(Long studentBranchId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        Long managerBranchId = currentUserDetails.getBranchId();
        
        if (managerBranchId == null) {
            throw new BusinessException("Manager must be assigned to a branch");
        }
        
        if (!managerBranchId.equals(studentBranchId)) {
            throw new BusinessException("Access denied. You can only manage payments from your branch.");
        }
    }
}
