package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.enums.*;
import com.lab.atlasmentor.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

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
    private DisputeRepository disputeRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
            throw new RuntimeException("Student payment already exists for student: " + studentId);
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
        
        // Only ADMIN and MANAGER can assign amounts
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can assign amounts.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Check if amount is already locked
        if (Boolean.TRUE.equals(studentPayment.getIsAmountLocked())) {
            throw new RuntimeException("Amount is locked. Cannot modify without approval.");
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
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public PaymentTransaction addPaymentTransaction(Long studentId, BigDecimal amount, PaymentMethod paymentMethod, 
                                                   String transactionReference, String notes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can add payment transactions
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can add payment transactions.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be positive.");
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
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public PaymentAmountChange requestAmountChange(Long studentId, BigDecimal newAmount, String remarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can request amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can request amount changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Check if there's already a pending request
        Optional<PaymentAmountChange> existingRequest = paymentAmountChangeRepository.findPendingByStudentId(studentId);
        if (existingRequest.isPresent()) {
            throw new RuntimeException("There is already a pending amount change request for this student.");
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
            throw new RuntimeException("Approval configuration not found for amount changes.");
        }

        PaymentAmountChange amountChange = paymentAmountChangeRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Amount change request not found: " + changeRequestId));

        // Validate access based on role and student
        validateAmountChangeAccess(amountChange, userRole, currentUserDetails.getUserId());

        if (!amountChange.isPending()) {
            throw new RuntimeException("This request is already processed.");
        }

        // Check if user can approve based on configuration
        if (!canApproveRequest(amountChange, config.get(), currentUserDetails.getUserId(), userRole)) {
            throw new RuntimeException("You don't have sufficient privileges to approve this request.");
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
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public StudentStatusApproval requestStatusChange(Long studentId, StudentStatusEnhanced newStatus, String reason, String proofUrl) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can request status changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can request status changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Reason is mandatory for rejection requests
        if (StudentStatusEnhanced.REJECTED.equals(newStatus) && (reason == null || reason.trim().isEmpty())) {
            throw new RuntimeException("Reason is mandatory for rejection requests.");
        }

        // For REJECTED status, create REJECTED_PENDING instead
        StudentStatusEnhanced requestedStatus = StudentStatusEnhanced.REJECTED.equals(newStatus) 
                ? StudentStatusEnhanced.REJECTED_PENDING 
                : newStatus;

        // Check if there's already a pending request for this status
        Optional<StudentStatusApproval> existingRequest = studentStatusApprovalRepository
                .findPendingByStudentIdAndRequestedStatus(studentId, requestedStatus);
        if (existingRequest.isPresent()) {
            throw new RuntimeException("There is already a pending status change request for this student.");
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
            throw new RuntimeException("Approval configuration not found for status changes.");
        }

        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Validate access based on role and student source
        validateStatusApprovalAccess(statusApproval, userRole, currentUserDetails.getUserId());

        if (!statusApproval.isPending()) {
            throw new RuntimeException("This request is already processed.");
        }

        // Check if user can approve based on configuration
        if (!canApproveRequest(statusApproval, config.get(), currentUserDetails.getUserId(), userRole)) {
            throw new RuntimeException("You don't have sufficient privileges to approve this request.");
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
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    @Transactional
    public Dispute raiseDispute(Long approvalRequestId, String disputeReason) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Only REFERRAL and COMPANY can raise disputes for their own students
        if (!("REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only REFERRAL and COMPANY can raise disputes.");
        }

        // Validate that the user owns this student
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(statusApproval.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if (!canAccessStudent(studentPayment, userRole, currentUserDetails.getUserId())) {
            throw new RuntimeException("Access denied. You can only raise disputes for your own students.");
        }

        // Create dispute
        Dispute dispute = new Dispute(statusApproval.getStudent(), statusApproval, currentUserDetails.getUserId(), disputeReason);
        
        Dispute savedDispute = disputeRepository.save(dispute);
        
        // Update student status to DISPUTED if it was REJECTED_PENDING
        if (StudentStatusEnhanced.REJECTED_PENDING.equals(statusApproval.getRequestedStatus())) {
            Student student = statusApproval.getStudent();
            student.setEnhancedStatus(StudentStatusEnhanced.DISPUTED);
            student.setUpdatedBy(currentUserDetails.getUserId());
            studentRepository.save(student);
        }
        
        // Create audit log
        createAuditLog(statusApproval.getStudent(), PaymentAuditAction.REJECTION_REJECTED, 
                      "Dispute raised against: " + statusApproval.getRequestedStatus(), 
                      "DISPUTED", "Dispute", 
                      currentUserDetails.getUserId(), disputeReason);
        
        return savedDispute;
    }

    @Transactional
    public Dispute resolveDispute(Long disputeId, String resolutionNotes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can resolve disputes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can resolve disputes.");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        if (!dispute.isOpen() && !dispute.isInProgress()) {
            throw new RuntimeException("This dispute cannot be resolved.");
        }

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(dispute.getStudent().getBranch() != null ? dispute.getStudent().getBranch().getId() : null);
        }

        // Resolve the dispute
        dispute.resolve(currentUserDetails.getUserId(), resolutionNotes);
        disputeRepository.save(dispute);
        
        // Create audit log
        createAuditLog(dispute.getStudent(), PaymentAuditAction.REJECTION_APPROVED, 
                      "DISPUTED", "RESOLVED", "Dispute", 
                      currentUserDetails.getUserId(), resolutionNotes);
        
        return dispute;
    }

    // Helper methods
    private void validateManagerBranchAccess(Long studentBranchId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        Long managerBranchId = currentUserDetails.getBranchId();
        
        if (managerBranchId == null) {
            throw new RuntimeException("Manager must be assigned to a branch");
        }
        
        if (!managerBranchId.equals(studentBranchId)) {
            throw new RuntimeException("Access denied. You can only manage payments from your branch.");
        }
    }

    private void validateStatusApprovalAccess(StudentStatusApproval statusApproval, String userRole, Long userId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(statusApproval.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return; // Admin can approve all
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            if (!SourceType.REFERRAL.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new RuntimeException("Access denied. You can only approve your own student requests.");
            }
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            if (!SourceType.COMPANY.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new RuntimeException("Access denied. You can only approve your own student requests.");
            }
        }
    }

    private void validateAmountChangeAccess(PaymentAmountChange amountChange, String userRole, Long userId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(amountChange.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return; // Admin can approve all
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        } else {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER can approve amount changes.");
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
        try {
            // Create structured audit data
            Map<String, Object> oldData = oldValue != null ? Map.of("value", oldValue) : null;
            Map<String, Object> newData = newValue != null ? Map.of("value", newValue) : null;
            
            String oldJson = oldData != null ? objectMapper.writeValueAsString(oldData) : null;
            String newJson = newData != null ? objectMapper.writeValueAsString(newData) : null;
            
            PaymentAudit audit = new PaymentAudit(student, action, oldValue, newValue, 
                                                oldJson, newJson, entityName, doneBy, null, remarks);
            paymentAuditRepository.save(audit);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
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
            throw new RuntimeException("Access denied");
        }
    }

    public List<PaymentTransaction> getPaymentTransactions(Long studentId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if (!canAccessStudent(studentPayment, userRole, currentUserDetails.getUserId())) {
            throw new RuntimeException("Access denied");
        }
        
        return paymentTransactionRepository.findActiveByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public BigDecimal getTotalPaidAmount(Long studentId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));
        
        return studentPayment.calculateTotalPaid();
    }
}
