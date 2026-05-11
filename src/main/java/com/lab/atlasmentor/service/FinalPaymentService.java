package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.PaymentDisputeActivityDto;
import com.lab.atlasmentor.dto.PaymentTransactionDto;
import com.lab.atlasmentor.dto.UserInfoDto;
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
import java.util.UUID;

@Service
public class FinalPaymentService {

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
    private PaymentDisputeActivityRepository paymentDisputeActivityRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ClientPayoutService clientPayoutService;

    // ==================== STUDENT PAYMENT MANAGEMENT ====================

    @Transactional
    public StudentPayment createStudentPayment(Long studentId, SourceType sourceType, Long sourceId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String requestId = generateRequestId();
        
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
        
        // Create client payout if source type is REFERRAL or COMPANY
        if (sourceType == SourceType.REFERRAL || sourceType == SourceType.COMPANY) {
            try {
                clientPayoutService.createClientPayoutForStudent(student);
            } catch (Exception e) {
                // Log error but don't fail the payment creation
                System.err.println("Failed to create client payout for student " + studentId + ": " + e.getMessage());
            }
        }
        
        // Create audit log
        createAuditLog(student, PaymentAuditAction.STUDENT_CREATED, 
                      null, "Payment record created", "StudentPayment", 
                      currentUserDetails.getUserId(), requestId, null);
        
        return savedPayment;
    }

    @Transactional
    public StudentPayment assignAmount(Long studentId, BigDecimal amount) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can assign amounts
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can assign amounts.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
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
                          currentUserDetails.getUserId(), requestId, null);
            
            return savedPayment;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    // ==================== PAYMENT TRANSACTIONS ====================

    @Transactional
    public PaymentTransaction addPaymentTransaction(Long studentId, BigDecimal amount, PaymentMethod paymentMethod, 
                                                   TransactionType transactionType, String transactionReference, String notes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can add payment transactions
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can add payment transactions.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
            validateManagerBranchAccess(studentPayment.getBranchId());
        }

        // Prevent payments if already fully paid
        if (studentPayment.getPaymentStatus() == StudentPaymentStatus.PAID) {
            throw new RuntimeException("Payment is already fully paid. No further payments can be made.");
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be positive.");
        }

        // Overpayment protection for CREDIT transactions
        if (TransactionType.CREDIT.equals(transactionType)) {
            BigDecimal currentTotal = calculateNetPaidAmount(studentPayment);
            BigDecimal newTotal = currentTotal.add(amount);
            
            if (studentPayment.getAssignedAmount() != null && newTotal.compareTo(studentPayment.getAssignedAmount()) > 0) {
                throw new RuntimeException("Transaction would exceed assigned amount. Current total: " + currentTotal + 
                                         ", Transaction amount: " + amount + ", Assigned amount: " + studentPayment.getAssignedAmount());
            }
        }

        // Create payment transaction
        PaymentTransaction transaction = new PaymentTransaction(
                studentPayment.getStudent(), 
                studentPayment, 
                amount, 
                paymentMethod, 
                transactionType,
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
                          "Previous net total: " + calculateNetPaidAmount(studentPayment).subtract(
                              TransactionType.CREDIT.equals(transactionType) ? amount : amount.negate()), 
                          "New net total: " + calculateNetPaidAmount(studentPayment), 
                          "PaymentTransaction", currentUserDetails.getUserId(), requestId, notes);
            
            return savedTransaction;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    // ==================== AMOUNT CHANGE WORKFLOW ====================

    @Transactional
    public PaymentAmountChange requestAmountChange(Long studentId, BigDecimal newAmount, String remarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request amount changes.");
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
                      currentUserDetails.getUserId(), requestId, remarks);
        
        return savedRequest;
    }

    @Transactional
    public PaymentAmountChange approveAmountChange(Long changeRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Check approval configuration
        Optional<ApprovalConfig> config = approvalConfigRepository.findActiveByActionType(ApprovalActionType.AMOUNT_CHANGE);
        if (config.isEmpty()) {
            throw new RuntimeException("Approval configuration not found for amount changes.");
        }

        PaymentAmountChange amountChange = paymentAmountChangeRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Amount change request not found: " + changeRequestId));

        // Self-approval prevention
        if (amountChange.getRequestedBy().equals(currentUserDetails.getUserId())) {
            throw new RuntimeException("Self-approval is not allowed. You cannot approve your own requests.");
        }

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
                          currentUserDetails.getUserId(), requestId, approvalRemarks);
            
            return amountChange;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    // ==================== STATUS CHANGE WORKFLOW ====================

    @Transactional
    public StudentStatusApproval requestStatusChange(Long studentId, StudentStatusEnhanced newStatus, String reason, String proofUrl) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes
        if (!("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole))) {
            throw new RuntimeException("Access denied. Only ADMIN and MANAGER/BRANCH_PARTNER can request status changes.");
        }

        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found for student: " + studentId));

        // Validate manager branch access if user is MANAGER or BRANCH_PARTNER
        if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole)) {
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
                      currentUserDetails.getUserId(), requestId, reason);
        
        return savedRequest;
    }

    @Transactional
    public StudentStatusApproval approveStatusChange(Long approvalRequestId, String approvalRemarks) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Check approval configuration
        Optional<ApprovalConfig> config = approvalConfigRepository.findActiveByActionType(ApprovalActionType.REJECTION);
        if (config.isEmpty()) {
            throw new RuntimeException("Approval configuration not found for status changes.");
        }

        StudentStatusApproval statusApproval = studentStatusApprovalRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Status approval request not found: " + approvalRequestId));

        // Self-approval prevention
        if (statusApproval.getRequestedBy().equals(currentUserDetails.getUserId())) {
            throw new RuntimeException("Self-approval is not allowed. You cannot approve your own requests.");
        }

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
                          currentUserDetails.getUserId(), requestId, approvalRemarks);
            
            return statusApproval;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Concurrent modification detected. Please refresh and try again.", e);
        }
    }

    
    // ==================== REPORTING METHODS ====================

    public ReportingData getReportingData(Long sourceId, SourceType sourceType) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        if (!("ADMIN".equalsIgnoreCase(userRole) || 
              (sourceType == SourceType.REFERRAL && "REFERRAL".equalsIgnoreCase(userRole) && sourceId.equals(currentUserDetails.getUserId())) ||
              (sourceType == SourceType.COMPANY && "COMPANY".equalsIgnoreCase(userRole) && sourceId.equals(currentUserDetails.getUserId())))) {
            throw new RuntimeException("Access denied");
        }

        List<StudentPayment> payments = studentPaymentRepository.findBySourceIdAndSourceType(sourceId, sourceType);
        
        BigDecimal totalAssigned = payments.stream()
                .filter(payment -> payment.getAssignedAmount() != null)
                .map(StudentPayment::getAssignedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPaid = payments.stream()
                .map(this::calculateNetPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal pendingAmount = totalAssigned.subtract(totalPaid);
        
        long totalStudents = payments.size();
        
        return new ReportingData(totalStudents, totalAssigned, totalPaid, pendingAmount);
    }

    // ==================== HELPER METHODS ====================

    private BigDecimal calculateNetPaidAmount(StudentPayment studentPayment) {
        // Use repository to avoid lazy loading issues
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findActiveByPaymentIdOrderByCreatedAtDesc(studentPayment.getId());
        
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal creditTotal = transactions.stream()
                .filter(t -> TransactionType.CREDIT.equals(t.getTransactionType()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal debitTotal = transactions.stream()
                .filter(t -> TransactionType.DEBIT.equals(t.getTransactionType()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return creditTotal.subtract(debitTotal);
    }

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

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void createAuditLog(Student student, PaymentAuditAction action, String oldValue, String newValue, 
                               String entityName, Long doneBy, String requestId, String remarks) {
        try {
            // Create structured audit data
            Map<String, Object> oldData = oldValue != null ? Map.of("value", oldValue) : null;
            Map<String, Object> newData = newValue != null ? Map.of("value", newValue) : null;
            
            String oldJson = oldData != null ? objectMapper.writeValueAsString(oldData) : null;
            String newJson = newData != null ? objectMapper.writeValueAsString(newData) : null;
            
            PaymentAudit audit = new PaymentAudit(student, action, oldValue, newValue, 
                                                oldJson, newJson, entityName, doneBy, requestId, remarks);
            paymentAuditRepository.save(audit);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }

    // ==================== QUERY METHODS ====================

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

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getPaymentTransactions(Long studentId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();

        // Validate access
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));

        if (!canAccessStudent(studentPayment, userRole, currentUserDetails.getUserId())) {
            throw new RuntimeException("Access denied");
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository.findActiveByStudentIdOrderByCreatedAtDesc(studentId);

        // Get dispute activities for this payment
        List<PaymentDisputeActivity> disputeActivities = paymentDisputeActivityRepository.findByPaymentIdOrderByDoneAtDesc(studentPayment.getId());

        // Convert dispute activities to DTOs
        List<PaymentDisputeActivityDto> disputeDtos = disputeActivities.stream()
                .map(this::convertToDisputeDto)
                .toList();

        // Convert transactions to DTOs
        return transactions.stream()
                .map(t -> convertToTransactionDto(t, disputeDtos))
                .toList();
    }

    private PaymentTransactionDto convertToTransactionDto(PaymentTransaction transaction, List<PaymentDisputeActivityDto> disputeActivities) {
        String createdByName = null;
        if (transaction.getCreatedBy() != null) {
            createdByName = userRepository.findById(transaction.getCreatedBy())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse(null);
        }

        PaymentTransactionDto dto = new PaymentTransactionDto(
                transaction.getId(),
                transaction.getStudent() != null ? transaction.getStudent().getId() : null,
                transaction.getStudentPayment() != null ? transaction.getStudentPayment().getId() : null,
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getTransactionType(),
                transaction.getTransactionReference(),
                transaction.getNotes(),
                transaction.getCreatedBy(),
                createdByName,
                transaction.getCreatedAt()
        );
        dto.setDisputeActivities(disputeActivities);
        return dto;
    }

    private PaymentDisputeActivityDto convertToDisputeDto(PaymentDisputeActivity activity) {
        UserInfoDto doneByDto = null;
        if (activity.getDoneBy() != null) {
            User user = activity.getDoneBy();
            doneByDto = new UserInfoDto(
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getName() : null
            );
        }

        return new PaymentDisputeActivityDto(
                activity.getId(),
                activity.getPayment() != null ? activity.getPayment().getId() : null,
                activity.getAction(),
                activity.getOldValue(),
                activity.getNewValue(),
                activity.getReason(),
                doneByDto,
                activity.getDoneAt()
        );
    }

    public BigDecimal getNetPaidAmount(Long studentId) {
        StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student payment not found"));
        
        return calculateNetPaidAmount(studentPayment);
    }

    // ==================== DTO CLASSES ====================

    public static class ReportingData {
        private final long totalStudents;
        private final BigDecimal totalAssignedAmount;
        private final BigDecimal totalPaidAmount;
        private final BigDecimal pendingAmount;

        public ReportingData(long totalStudents, BigDecimal totalAssignedAmount, BigDecimal totalPaidAmount, BigDecimal pendingAmount) {
            this.totalStudents = totalStudents;
            this.totalAssignedAmount = totalAssignedAmount;
            this.totalPaidAmount = totalPaidAmount;
            this.pendingAmount = pendingAmount;
        }

        public long getTotalStudents() { return totalStudents; }
        public BigDecimal getTotalAssignedAmount() { return totalAssignedAmount; }
        public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
        public BigDecimal getPendingAmount() { return pendingAmount; }
    }
}
