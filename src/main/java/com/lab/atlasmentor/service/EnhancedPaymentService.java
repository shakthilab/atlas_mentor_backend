package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.enums.*;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EnhancedPaymentService {

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Autowired
    private PaymentAmountChangeRepository paymentAmountChangeRepository;

    @Autowired
    private StudentStatusApprovalRepository studentStatusApprovalRepository;

    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Autowired
    private FinancialAuditService financialAuditService;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public StudentPayment createStudentPayment(Long studentId, SourceType sourceType, Long sourceId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        
        // Validate student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        // Check if payment already exists
        Optional<StudentPayment> existingPayment = studentPaymentRepository.findByStudentId(studentId);
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
                      null, "Payment record created", currentUserDetails.getUserId());
        
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

        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(studentId)
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
        
        StudentPayment savedPayment = studentPaymentRepository.save(studentPayment);
        
        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.AMOUNT_ASSIGNED, 
                      oldAmount != null ? oldAmount.toString() : null, 
                      amount.toString(), currentUserDetails.getUserId());
        
        return savedPayment;
    }

    @Transactional
    public PaymentAmountChange requestAmountChange(Long studentId, BigDecimal newAmount, String remarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(studentId)
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
                      newAmount.toString(), currentUserDetails.getUserId(), remarks);
        
        return savedRequest;
    }

    @Transactional
    public PaymentAmountChange approveAmountChange(Long changeRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can approve amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER can approve amount changes.");
        }

        PaymentAmountChange amountChange = paymentAmountChangeRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Amount change request not found: " + changeRequestId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(amountChange.getStudent().getBranch() != null ? amountChange.getStudent().getBranch().getId() : null);
        }

        if (!amountChange.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Approve the request
        amountChange.approve(currentUserDetails.getUserId(), approvalRemarks);
        paymentAmountChangeRepository.save(amountChange);

        // Update the student payment amount
        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(amountChange.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        BigDecimal oldAmount = studentPayment.getAssignedAmount();
        studentPayment.setAssignedAmount(amountChange.getNewAmount());
        studentPayment.setUpdatedBy(currentUserDetails.getUserId());
        studentPaymentRepository.save(studentPayment);

        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.AMOUNT_CHANGE_APPROVED, 
                      oldAmount != null ? oldAmount.toString() : null, 
                      amountChange.getNewAmount().toString(), currentUserDetails.getUserId(), approvalRemarks);
        
        return amountChange;
    }

    @Transactional
    public PaymentAmountChange rejectAmountChange(Long changeRequestId, String rejectionReason) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER can reject amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER can reject amount changes.");
        }

        PaymentAmountChange amountChange = paymentAmountChangeRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Amount change request not found: " + changeRequestId));

        // Validate manager branch access if user is MANAGER
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(amountChange.getStudent().getBranch() != null ? amountChange.getStudent().getBranch().getId() : null);
        }

        if (!amountChange.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Reject the request
        amountChange.reject(currentUserDetails.getUserId(), rejectionReason);
        paymentAmountChangeRepository.save(amountChange);

        // Create audit log
        createAuditLog(amountChange.getStudent(), PaymentAuditAction.AMOUNT_CHANGE_APPROVED, 
                      amountChange.getOldAmount() != null ? amountChange.getOldAmount().toString() : null, 
                      amountChange.getNewAmount().toString(), currentUserDetails.getUserId(), "Rejected: " + rejectionReason);
        
        return amountChange;
    }

    @Transactional
    public StudentStatusApproval requestStatusChange(Long studentId, StudentStatusEnhanced newStatus, String remarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
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
        statusApproval.setRemarks(remarks);
        
        StudentStatusApproval savedRequest = studentStatusApprovalRepository.save(statusApproval);
        
        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.REJECTION_REQUESTED, 
                      studentPayment.getStudentStatus().toString(), 
                      requestedStatus.toString(), currentUserDetails.getUserId(), remarks);
        
        return savedRequest;
    }

    @Transactional
    public StudentStatusApproval approveStatusChange(Long approvalRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN, MANAGER/BRANCH_PARTNER, REFERRAL, and COMPANY can approve based on context
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) ||
              "REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied.");
        }

        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Validate access based on role and student source
        validateStatusApprovalAccess(statusApproval, userRole, currentUserDetails.getUserId());

        if (!statusApproval.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Approve the request
        statusApproval.approve(currentUserDetails.getUserId(), approvalRemarks);
        studentStatusApprovalRepository.save(statusApproval);

        // Update the student payment status
        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(statusApproval.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        StudentStatusEnhanced oldStatus = studentPayment.getStudentStatus();
        studentPayment.setStudentStatus(statusApproval.getRequestedStatus());
        studentPayment.setUpdatedBy(currentUserDetails.getUserId());
        studentPaymentRepository.save(studentPayment);

        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.REJECTION_APPROVED, 
                      oldStatus.toString(), 
                      statusApproval.getRequestedStatus().toString(), currentUserDetails.getUserId(), approvalRemarks);
        
        return statusApproval;
    }

    @Transactional
    public StudentStatusApproval rejectStatusChange(Long approvalRequestId, String rejectionReason) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN, MANAGER/BRANCH_PARTNER, REFERRAL, and COMPANY can reject based on context
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) ||
              "REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied.");
        }

        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Validate access based on role and student source
        validateStatusApprovalAccess(statusApproval, userRole, currentUserDetails.getUserId());

        if (!statusApproval.isPending()) {
            throw new BusinessException("This request is already processed.");
        }

        // Reject the request
        statusApproval.reject(currentUserDetails.getUserId(), rejectionReason);
        studentStatusApprovalRepository.save(statusApproval);

        
        // Create audit log
        createAuditLog(statusApproval.getStudent(), PaymentAuditAction.REJECTION_REJECTED, 
                      statusApproval.getRequestedStatus().toString(), 
                      "REJECTED", currentUserDetails.getUserId(), rejectionReason);
        
        return statusApproval;
    }

    @Transactional
    public StudentPayment updatePaymentAmount(Long studentId, BigDecimal amount) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can update payment amounts
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can update payment amounts.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        BigDecimal oldPaidAmount = studentPayment.getPaidAmount();
        studentPayment.setPaidAmount(amount);
        studentPayment.setUpdatedBy(currentUserDetails.getUserId());
        
        // Auto-update payment status
        studentPayment.updatePaymentStatus();
        
        StudentPayment savedPayment = studentPaymentRepository.save(studentPayment);
        
        // Create audit log
        createAuditLog(studentPayment.getStudent(), PaymentAuditAction.PAYMENT_UPDATED, 
                      oldPaidAmount.toString(), 
                      amount.toString(), currentUserDetails.getUserId());
        
        return savedPayment;
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
        StudentPayment studentPayment = studentPaymentRepository.findByStudentId(statusApproval.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin can approve all
            return;
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            // Manager/Branch Partner can approve within their branch
            validateManagerBranchAccess(studentPayment.getBranchId());
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Referral can only approve if they are the source
            if (!SourceType.REFERRAL.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new BusinessException("Access denied. You can only approve your own student requests.");
            }
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Company can only approve if they are the source
            if (!SourceType.COMPANY.equals(studentPayment.getSourceType()) || 
                !userId.equals(studentPayment.getSourceId())) {
                throw new BusinessException("Access denied. You can only approve your own student requests.");
            }
        }
    }

    private void createAuditLog(Student student, PaymentAuditAction action, String oldValue, String newValue, Long doneBy) {
        createAuditLog(student, action, oldValue, newValue, doneBy, null);
    }

    private void createAuditLog(Student student, PaymentAuditAction action, String oldValue, String newValue, Long doneBy, String remarks) {
        // Tamper-evident audit — must succeed; failure rolls back the enclosing transaction.
        financialAuditService.record(
                toFinancialAction(action),
                "StudentPayment",
                student.getId(),
                doneBy,
                oldValue,
                newValue,
                remarks);

        // Legacy PaymentAudit — best-effort.
        try {
            PaymentAudit audit = new PaymentAudit(student, action, oldValue, newValue, doneBy, remarks);
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
            return studentPaymentRepository.findAll();
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findByBranchIdOrderByCreatedAtDesc(currentUserDetails.getBranchId());
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.REFERRAL);
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            return studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.COMPANY);
        } else {
            throw new BusinessException("Access denied");
        }
    }

    public List<PaymentAmountChange> getPendingAmountChanges() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new BusinessException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can view pending amount changes.");
        }

        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            // Get pending changes for manager's/branch partner's branch
            List<StudentPayment> managerPayments = studentPaymentRepository.findByBranchIdOrderByCreatedAtDesc(currentUserDetails.getBranchId());
            return managerPayments.stream()
                    .flatMap(payment -> paymentAmountChangeRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(payment.getStudent().getId(), ApprovalStatus.PENDING).stream())
                    .toList();
        } else {
            return paymentAmountChangeRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING);
        }
    }

    public List<StudentStatusApproval> getPendingStatusApprovals() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return studentStatusApprovalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING);
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            // Get pending approvals for manager's branch
            List<StudentPayment> managerPayments = studentPaymentRepository.findByBranchIdOrderByCreatedAtDesc(currentUserDetails.getBranchId());
            return managerPayments.stream()
                    .flatMap(payment -> studentStatusApprovalRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(payment.getStudent().getId(), ApprovalStatus.PENDING).stream())
                    .toList();
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Get pending approvals for referral's students
            List<StudentPayment> referralPayments = studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.REFERRAL);
            return referralPayments.stream()
                    .flatMap(payment -> studentStatusApprovalRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(payment.getStudent().getId(), ApprovalStatus.PENDING).stream())
                    .toList();
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Get pending approvals for company's students
            List<StudentPayment> companyPayments = studentPaymentRepository.findBySourceIdAndSourceType(currentUserDetails.getUserId(), SourceType.COMPANY);
            return companyPayments.stream()
                    .flatMap(payment -> studentStatusApprovalRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(payment.getStudent().getId(), ApprovalStatus.PENDING).stream())
                    .toList();
        } else {
            throw new BusinessException("Access denied");
        }
    }
}
