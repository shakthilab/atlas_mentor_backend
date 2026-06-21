package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.enums.*;
import com.lab.atlasmentor.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Slf4j
@Service
public class EnhancedPaymentServiceV2 {

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private PaymentAmountChangeRepository paymentAmountChangeRepository;
    
    @Autowired
    private StudentStatusApprovalRepository studentStatusApprovalRepository;
    
    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Autowired
    private ApprovalConfigRepository approvalConfigRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialAuditService financialAuditService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public StudentPayment createStudentPayment(Long studentId, SourceType sourceType, Long sourceId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        
        // Validate student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        // Check if payment already exists
        Optional<StudentPayment> existingPayment = studentPaymentRepository.findActiveByStudentId(studentId);
        if (existingPayment.isPresent()) {
            throw new BusinessException("Student payment already exists for student: " + studentId);
        }

        // Create student payment
        StudentPayment studentPayment = new StudentPayment(student, sourceType, sourceId);
        studentPayment.setCreatedBy(currentUserDetails.getUserId());
        
        // Update student source information
        student.setSourceType(sourceType);
        student.setSourceId(sourceId);
        studentRepository.save(student);
        
        // Save payment
        StudentPayment savedPayment = studentPaymentRepository.save(studentPayment);
        
        // Create audit log
        createAuditLog(student, PaymentAuditAction.STUDENT_CREATED, 
                      null, "Payment record created", "StudentPayment", 
                      currentUserDetails.getUserId(), null);
        
        return savedPayment;
    }

    @Transactional
    public StudentPayment assignAmount(Long studentId, BigDecimal amount) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can assign amounts
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can assign amounts.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Check if amount is already locked
        if (Boolean.TRUE.equals(studentPayment.getIsAmountLocked())) {
            throw new BusinessException("Amount is locked. Cannot modify without approval.");
        }

        BigDecimal oldAmount = studentPayment.getAssignedAmount();
        studentPayment.setAssignedAmount(amount);
        studentPayment.setUpdatedBy(currentUserDetails.getUserId());
        
        // Lock the amount after assignment
        studentPayment.setIsAmountLocked(true);
        
        try {
            StudentPayment savedPayment = studentPaymentRepository.save(studentPayment);
            
            // Create audit log with JSON data
            createAuditLog(studentPayment.getStudent(), PaymentAuditAction.AMOUNT_ASSIGNED, 
                          oldAmount != null ? oldAmount.toString() : null, 
                          amount.toString(), "StudentPayment", 
                          currentUserDetails.getUserId(), null);
            
            return savedPayment;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public PaymentTransaction addPaymentTransaction(Long studentId, BigDecimal amount, PaymentMethod paymentMethod, 
                                                   String transactionReference, String notes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can add payment transactions
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can add payment transactions.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive.");
        }

        // Create payment transaction
        PaymentTransaction transaction = new PaymentTransaction(
                studentPayment.getStudent(), 
                studentPayment, 
                amount, 
                paymentMethod, 
                TransactionType.CREDIT, 
                transactionReference, 
                currentUserDetails.getUserId()
        );
        transaction.setNotes(notes);
        
        try {
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
            
            // Update payment status based on transactions
            studentPayment.updatePaymentStatus();
            studentPayment.setUpdatedBy(currentUserDetails.getUserId());
            studentPaymentRepository.save(studentPayment);
            
            // Create audit log
            createAuditLog(studentPayment.getStudent(), PaymentAuditAction.PAYMENT_UPDATED, 
                          "Previous total: " + studentPayment.calculateTotalPaid().subtract(amount), 
                          "New total: " + studentPayment.calculateTotalPaid(), 
                          "PaymentTransaction", currentUserDetails.getUserId(), notes);
            
            return savedTransaction;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public PaymentAmountChange requestAmountChange(Long studentId, BigDecimal newAmount, String remarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Check if there's already a pending request
        Optional<PaymentAmountChange> existingRequest = paymentAmountChangeRepository.findPendingByStudentId(studentId);
        if (existingRequest.isPresent()) {
            throw new BusinessException("There is already a pending amount change request for this student.");
        }

        // Create amount change request
        PaymentAmountChange amountChange = new PaymentAmountChange(
                studentPayment.getStudent(), 
                studentPayment.getAssignedAmount(), 
                newAmount, 
                currentUserDetails.getUserId()
        );
        amountChange.setRemarks(remarks);
        
        PaymentAmountChange savedRequest = paymentAmountChangeRepository.save(amountChange);
        
        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.AMOUNT_CHANGE_REQUESTED, 
                      studentPayment.getAssignedAmount() != null ? studentPayment.getAssignedAmount().toString() : null, 
                      newAmount.toString(), "PaymentAmountChange", 
                      currentUserDetails.getUserId(), remarks);
        
        return savedRequest;
    }

    @Transactional
    public PaymentAmountChange approveAmountChange(Long changeRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Check approval configuration
        Optional<ApprovalConfig> config = approvalConfigRepository.findActiveByActionType(ApprovalActionType.AMOUNT_CHANGE);
        if (config.isEmpty()) {
            throw new BusinessException("Approval configuration not found for amount changes.");
        }

        PaymentAmountChange amountChange = paymentAmountChangeRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Amount change request not found: " + changeRequestId));

        // Validate access based on role and student
        validateAmountChangeAccess(amountChange, userRole, currentUserDetails.getUserId());

        if (!amountChange.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Check if user can approve based on configuration
        if (!canApproveRequest(amountChange, config.get(), currentUserDetails.getUserId(), userRole)) {
            throw new BusinessException("You don't have sufficient privileges to approve this request.");
        }

        try {
            // Approve the request
            amountChange.approve(currentUserDetails.getUserId(), approvalRemarks);
            paymentAmountChangeRepository.save(amountChange);

            // Update the student payment amount
            StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(amountChange.getStudent().getId())
                    .orElseThrow(() -> new RuntimeException("Student payment not found"));

            BigDecimal oldAmount = studentPayment.getAssignedAmount();
            studentPayment.setAssignedAmount(amountChange.getNewAmount());
            studentPayment.setUpdatedBy(currentUserDetails.getUserId());
            studentPaymentRepository.save(studentPayment);

            // Create audit log
            createAuditLog(studentPayment.getStudent(), PaymentAuditAction.AMOUNT_CHANGE_APPROVED, 
                          oldAmount != null ? oldAmount.toString() : null, 
                          amountChange.getNewAmount().toString(), "PaymentAmountChange", 
                          currentUserDetails.getUserId(), approvalRemarks);
            
            return amountChange;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public StudentStatusApproval requestStatusChange(Long studentId, StudentStatusEnhanced newStatus, String reason, String proofUrl) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Reason is mandatory for rejection requests
        if (StudentStatusEnhanced.REJECTED.equals(newStatus) && (reason == null || reason.trim().isEmpty())) {
            throw new BusinessException("Reason is mandatory for rejection requests.");
        }

        // For REJECTED status, create REJECTED_PENDING instead
        StudentStatusEnhanced requestedStatus = StudentStatusEnhanced.REJECTED.equals(newStatus) 
                ? StudentStatusEnhanced.REJECTED_PENDING 
                : newStatus;

        // Check if there's already a pending request for this status
        Optional<StudentStatusApproval> existingRequest = studentStatusApprovalRepository
                .findPendingByStudentIdAndRequestedStatus(studentId, requestedStatus);
        if (existingRequest.isPresent()) {
            throw new BusinessException("There is already a pending status change request for this student.");
        }

        // Create status change request
        StudentStatusApproval statusApproval = new StudentStatusApproval(
                studentPayment.getStudent(), 
                requestedStatus, 
                currentUserDetails.getUserId()
        );
        statusApproval.setReason(reason);
        statusApproval.setProofUrl(proofUrl);
        
        StudentStatusApproval savedRequest = studentStatusApprovalRepository.save(statusApproval);
        
        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.REJECTION_REQUESTED, 
                      studentPayment.getStudent().getEnhancedStatus().toString(), 
                      requestedStatus.toString(), "StudentStatusApproval", 
                      currentUserDetails.getUserId(), reason);
        
        return savedRequest;
    }

    @Transactional
    public StudentStatusApproval approveStatusChange(Long approvalRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Check approval configuration
        Optional<ApprovalConfig> config = approvalConfigRepository.findActiveByActionType(ApprovalActionType.REJECTION);
        if (config.isEmpty()) {
            throw new BusinessException("Approval configuration not found for status changes.");
        }

        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Validate access based on role and student source
        validateStatusApprovalAccess(statusApproval, userRole, currentUserDetails.getUserId());

        if (!statusApproval.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Check if user can approve based on configuration
        if (!canApproveRequest(statusApproval, config.get(), currentUserDetails.getUserId(), userRole)) {
            throw new BusinessException("You don't have sufficient privileges to approve this request.");
        }

        try {
            // Approve the request
            statusApproval.approve(currentUserDetails.getUserId(), approvalRemarks);
            studentStatusApprovalRepository.save(statusApproval);

            // Update the student status
            Student student = studentRepository.findById(statusApproval.getStudent().getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            StudentStatusEnhanced oldStatus = student.getEnhancedStatus();
            student.setEnhancedStatus(statusApproval.getRequestedStatus());
            student.setUpdatedBy(currentUserDetails.getUserId());
            studentRepository.save(student);

            // Create audit log
            createAuditLog(student, PaymentAuditAction.REJECTION_APPROVED, 
                          oldStatus.toString(), 
                          statusApproval.getRequestedStatus().toString(), "StudentStatusApproval", 
                          currentUserDetails.getUserId(), approvalRemarks);
            
            return statusApproval;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    
    // Helper methods
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

    private void validateStatusApprovalAccess(StudentStatusApproval statusApproval, String userRole, Long userId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(statusApproval.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return; // Admin can approve all
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            if (!SourceType.REFERRAL.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new BusinessException("Access denied. You can only approve your own student requests.");
            }
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            if (!SourceType.COMPANY.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new BusinessException("Access denied. You can only approve your own student requests.");
            }
        }
    }

    private void validateAmountChangeAccess(PaymentAmountChange amountChange, String userRole, Long userId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(amountChange.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return; // Admin can approve all
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        } else {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can approve amount changes.");
        }
    }

    private boolean canAccessStudent(StudentPayment studentPayment, String userRole, Long userId) {
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return true;
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            return SourceType.REFERRAL.equals(studentPayment.getSourceType()) && 
                   userId.equals(studentPayment.getSourceId());
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            return SourceType.COMPANY.equals(studentPayment.getSourceType()) && 
                   userId.equals(studentPayment.getSourceId());
        }
        return false;
    }

    private boolean canApproveRequest(Object request, ApprovalConfig config, Long userId, String userRole) {
        // For simplicity, implement basic approval logic
        // In a real implementation, this would check existing approvals, roles, etc.
        return true; // Simplified for now
    }

    private void createAuditLog(Student student, PaymentAuditAction action, String oldValue, String newValue,
                               String entityName, Long doneBy, String remarks) {
        // Tamper-evident audit — must succeed; failure rolls back the enclosing transaction.
        financialAuditService.record(
                toFinancialAction(action),
                entityName,
                student.getId(),
                doneBy,
                oldValue,
                newValue,
                remarks);

        // Legacy PaymentAudit — best-effort.
        try {
            Map<String, Object> oldData = oldValue != null ? Map.of("value", oldValue) : null;
            Map<String, Object> newData = newValue != null ? Map.of("value", newValue) : null;
            String oldJson = oldData != null ? objectMapper.writeValueAsString(oldData) : null;
            String newJson = newData != null ? objectMapper.writeValueAsString(newData) : null;
            PaymentAudit audit = new PaymentAudit(student, action, oldValue, newValue,
                                                  oldJson, newJson, entityName, doneBy, null, remarks);
            paymentAuditRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to write legacy PaymentAudit for action {}: {}", action, e.getMessage());
        }
    }

    private FinancialAuditAction toFinancialAction(PaymentAuditAction action) {
        return switch (action) {
            case STUDENT_CREATED         -> FinancialAuditAction.PAYMENT_RECORD_CREATED;
            case AMOUNT_ASSIGNED         -> FinancialAuditAction.PAYMENT_AMOUNT_ASSIGNED;
            case PAYMENT_UPDATED         -> FinancialAuditAction.PAYMENT_TRANSACTION_ADDED;
            case AMOUNT_CHANGE_REQUESTED -> FinancialAuditAction.PAYMENT_AMOUNT_CHANGE_REQUESTED;
            case AMOUNT_CHANGE_APPROVED  -> FinancialAuditAction.PAYMENT_AMOUNT_CHANGE_APPROVED;
            case REJECTION_REQUESTED     -> FinancialAuditAction.PAYMENT_STATUS_CHANGE_REQUESTED;
            case REJECTION_APPROVED      -> FinancialAuditAction.PAYMENT_STATUS_CHANGE_APPROVED;
            case REJECTION_REJECTED      -> FinancialAuditAction.PAYMENT_STATUS_CHANGE_REJECTED;
            default                      -> FinancialAuditAction.PAYMENT_RECORD_CREATED;
        };
    }

    // Query methods
    public List<StudentPayment> getStudentPaymentsByRole() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findAllActiveOrderByCreatedAtDesc();
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findByBranchIdOrderByCreatedAtDesc(currentUserDetails.getBranchId())
                    .stream()
                    .filter(payment -> !payment.isPaymentDeleted())
                    .toList();
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.REFERRAL);
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.COMPANY);
        } else {
            throw new BusinessException("Access denied");
        }
    }

    public List<PaymentTransaction> getPaymentTransactions(Long studentId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if (!canAccessStudent(studentPayment, userRole, currentUserDetails.getUserId())) {
            throw new BusinessException("Access denied");
        }
        
        return paymentTransactionRepository.findActiveByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public BigDecimal getTotalPaidAmount(Long studentId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));
        
        return studentPayment.calculateTotalPaid();
    }
}
