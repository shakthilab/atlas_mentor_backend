package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.ClientPayoutDto;
import com.lab.atlasmentor.dto.ClientPayoutActivityDto;
import com.lab.atlasmentor.enums.ClientPayoutAction;
import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.DisputeStage;
import com.lab.atlasmentor.enums.FinancialAuditAction;
import com.lab.atlasmentor.enums.SourceType;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientPayoutService {

    @Autowired
    private ClientPayoutRepository clientPayoutRepository;
    
    @Autowired
    private ClientPayoutActivityRepository activityRepository;
    
    @Autowired
    private PaymentDisputeActivityService paymentDisputeActivityService;
    
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialAuditService financialAuditService;
    
    // ==================== CLIENT PAYOUT CREATION ====================

    @Transactional
    public ClientPayout createClientPayoutForStudent(Student student) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String requestId = generateRequestId();
        
        // Validate student has source information
        if (student.getSourceType() == null || student.getSourceId() == null) {
            throw new BusinessException("Student must have source type and source ID for client payout creation");
        }
        
        // Check if client payout already exists
        Optional<ClientPayout> existingPayout = clientPayoutRepository.findByStudentId(student.getId());
        if (existingPayout.isPresent()) {
            throw new BusinessException("Client payout already exists for student: " + student.getId());
        }
        
        // Get referral/company user
        User referralOrCompany = userRepository.findById(student.getSourceId())
                .orElseThrow(() -> new RuntimeException("Referral/Company not found with ID: " + student.getSourceId()));
        
        // Validate user role matches source type
        validateUserRoleMatchesSourceType(referralOrCompany, student.getSourceType());
        
        // Create client payout
        ClientPayout clientPayout = new ClientPayout(referralOrCompany, student, student.getSourceType());
        clientPayout.setCreatedBy(currentUserDetails.getUserId());
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(clientPayout);
        
        // Log creation activity
        logActivity(savedPayout, ClientPayoutAction.CREATED, null, null, 
                  "Client payout created for student", currentUserDetails.getUserId());
        
        return savedPayout;
    }
    
    // ==================== AMOUNT ASSIGNMENT ====================

    @Transactional
    public ClientPayout assignAmount(Long payoutId, BigDecimal amount, String notes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        String requestId = generateRequestId();
        
        // Validate access
        validateAmountAssignmentAccess(userRole);
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Assigned amount must be greater than zero");
        }
        
        // Track previous state
        BigDecimal oldAmount = payout.getAssignedAmount() != null ? payout.getAssignedAmount() : BigDecimal.ZERO;
        ClientPayoutStatus oldStatus = payout.getPayoutStatus();
        
        // Update payout
        payout.setAssignedAmount(amount);
        payout.setAssignedBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setAssignedAt(LocalDateTime.now());
        payout.setNotes(notes);
        payout.setDisputeAmount(amount); // Set dispute amount to assigned amount
        
        // Update status
        payout.updateStatusBasedOnPayment();
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Log activity
        ClientPayoutActivity activity = new ClientPayoutActivity(
            savedPayout, ClientPayoutAction.AMOUNT_ASSIGNED,
            oldAmount, amount,
            notes, userRepository.findById(currentUserDetails.getUserId()).orElse(null)
        );
        activityRepository.save(activity);
        
        // Log dispute activity for payment assignment
        try {
            // Find corresponding StudentPayment for this payout
            Optional<StudentPayment> studentPaymentOpt = studentPaymentRepository.findByStudentIdAndSourceType(
                savedPayout.getStudent().getId(), savedPayout.getSourceType());
                
            if (studentPaymentOpt.isPresent()) {
                User currentUser = userRepository.findById(currentUserDetails.getUserId()).orElse(null);
                paymentDisputeActivityService.logPaymentAssigned(studentPaymentOpt.get(), amount, currentUser);
            }
        } catch (Exception e) {
            // Log the error but don't fail the assignment
            System.err.println("Failed to log dispute activity: " + e.getMessage());
        }
        
        return savedPayout;
    }
    
    // ==================== PAYMENT PROCESSING ====================

    @Transactional
    public ClientPayout addPayment(Long payoutId, BigDecimal amount, String paymentMethod, 
                                String transactionReference, String notes) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        validatePaymentProcessingAccess(userRole);
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate payout status
        if (!payout.getPayoutStatus().equals(ClientPayoutStatus.AMOUNT_ASSIGNED) && 
            !payout.getPayoutStatus().equals(ClientPayoutStatus.PARTIAL_PAID)) {
            throw new BusinessException("Cannot add payment to payout in status: " + payout.getPayoutStatus());
        }
        
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        
        // Check for overpayment
        BigDecimal newPaidAmount = payout.getPaidAmount().add(amount);
        if (newPaidAmount.compareTo(payout.getAssignedAmount()) > 0) {
            throw new BusinessException("Payment amount exceeds assigned amount");
        }
        
        // Track previous state
        BigDecimal oldPaidAmount = payout.getPaidAmount();
        ClientPayoutStatus oldStatus = payout.getPayoutStatus();
        
        // Update payout
        payout.setPaidAmount(newPaidAmount);
        payout.setLastPaidBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setLastPaidAt(LocalDateTime.now());
        payout.setPaymentMethod(paymentMethod);
        payout.setTransactionReference(transactionReference);
        
        // Update status
        payout.updateStatusBasedOnPayment();
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Log activity
        ClientPayoutActivity activity = new ClientPayoutActivity(
            savedPayout, ClientPayoutAction.PAYMENT_ADDED,
            oldPaidAmount, newPaidAmount,
            notes, userRepository.findById(currentUserDetails.getUserId()).orElse(null)
        );
        activity.setPaymentMethod(paymentMethod);
        activity.setTransactionReference(transactionReference);
        activity.setPreviousStatus(oldStatus.name());
        activity.setNewStatus(savedPayout.getPayoutStatus().name());
        activityRepository.save(activity);
        
        // Log dispute activity for payment addition
        try {
            // Find corresponding StudentPayment for this payout
            Optional<StudentPayment> studentPaymentOpt = studentPaymentRepository.findByStudentIdAndSourceType(
                savedPayout.getStudent().getId(), savedPayout.getSourceType());
                
            if (studentPaymentOpt.isPresent()) {
                User currentUser = userRepository.findById(currentUserDetails.getUserId()).orElse(null);
                paymentDisputeActivityService.logPaymentAssigned(studentPaymentOpt.get(), newPaidAmount, currentUser);
            }
        } catch (Exception e) {
            // Log the error but don't fail the payment
            System.err.println("Failed to log dispute activity: " + e.getMessage());
        }
        
        return savedPayout;
    }
    
    // ==================== DISPUTE MANAGEMENT ====================

    @Transactional
    public ClientPayout initiateDispute(Long payoutId, String disputeReason) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        validateDisputeInitiationAccess(userRole);
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate status
        if (!canInitiateDispute(payout.getPayoutStatus())) {
            throw new BusinessException("Cannot initiate dispute for payout in status: " + payout.getPayoutStatus());
        }
        
        // Track previous status
        ClientPayoutStatus oldStatus = payout.getPayoutStatus();
        
        // Update dispute info
        payout.setPayoutStatus(ClientPayoutStatus.DISPUTE);
        payout.setPreviousStatus(oldStatus);
        payout.setDisputeReason(disputeReason);
        payout.setDisputedBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setDisputedAt(LocalDateTime.now());
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Tamper-evident audit for dispute initiation.
        financialAuditService.record(
                FinancialAuditAction.PAYOUT_DISPUTE_INITIATED,
                "ClientPayout", savedPayout.getId(),
                currentUserDetails.getUserId(),
                oldStatus.name(), ClientPayoutStatus.DISPUTE.name(), disputeReason);

        // Legacy activity log — kept for dispute stage / status detail.
        ClientPayoutActivity activity = new ClientPayoutActivity(
            savedPayout, ClientPayoutAction.DISPUTE_INITIATED,
            oldStatus.name(), ClientPayoutStatus.DISPUTE.name(),
            disputeReason, userRepository.findById(currentUserDetails.getUserId()).orElse(null)
        );
        activity.setDisputeStage(DisputeStage.INITIATED);
        activity.setPreviousStatus(oldStatus.name());
        activity.setNewStatus(ClientPayoutStatus.DISPUTE.name());
        activityRepository.save(activity);

        return savedPayout;
    }

    @Transactional
    public ClientPayout acceptDispute(Long payoutId, String response) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        validateDisputeResponseAccess(payoutId, userRole, currentUserDetails.getUserId());
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate status
        if (!payout.getPayoutStatus().equals(ClientPayoutStatus.DISPUTE)) {
            throw new BusinessException("Only disputed payouts can be accepted");
        }
        
        // Update response
        payout.setPayoutStatus(ClientPayoutStatus.ACCEPTED);
        payout.setDisputeResponse(response);
        payout.setRespondedBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setRespondedAt(LocalDateTime.now());
        payout.setSettledAmount(BigDecimal.ZERO);
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Tamper-evident audit for dispute acceptance.
        financialAuditService.record(
                FinancialAuditAction.PAYOUT_DISPUTE_ACCEPTED,
                "ClientPayout", savedPayout.getId(),
                currentUserDetails.getUserId(),
                ClientPayoutStatus.DISPUTE.name(), ClientPayoutStatus.ACCEPTED.name(), response);

        // Legacy activity log.
        ClientPayoutActivity activity = new ClientPayoutActivity(
            savedPayout, ClientPayoutAction.DISPUTE_ACCEPTED,
            ClientPayoutStatus.DISPUTE.name(), ClientPayoutStatus.ACCEPTED.name(),
            response, userRepository.findById(currentUserDetails.getUserId()).orElse(null)
        );
        activity.setDisputeStage(DisputeStage.ACCEPTED);
        activity.setPreviousStatus(ClientPayoutStatus.DISPUTE.name());
        activity.setNewStatus(ClientPayoutStatus.ACCEPTED.name());
        activityRepository.save(activity);

        return savedPayout;
    }

    @Transactional
    public ClientPayout rejectDispute(Long payoutId, String response) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        
        // Validate access
        validateDisputeResponseAccess(payoutId, userRole, currentUserDetails.getUserId());
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate status
        if (!payout.getPayoutStatus().equals(ClientPayoutStatus.DISPUTE)) {
            throw new BusinessException("Only disputed payouts can be rejected");
        }
        
        // Determine previous status
        ClientPayoutStatus previousStatus = determinePreviousStatus(payout);
        
        // Update response
        payout.setPayoutStatus(previousStatus);
        payout.setDisputeResponse(response);
        payout.setRespondedBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setRespondedAt(LocalDateTime.now());
        
        // Save payout
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Tamper-evident audit for dispute rejection.
        financialAuditService.record(
                FinancialAuditAction.PAYOUT_DISPUTE_REJECTED,
                "ClientPayout", savedPayout.getId(),
                currentUserDetails.getUserId(),
                ClientPayoutStatus.DISPUTE.name(), previousStatus.name(), response);

        // Legacy activity log.
        ClientPayoutActivity activity = new ClientPayoutActivity(
            savedPayout, ClientPayoutAction.DISPUTE_REJECTED,
            ClientPayoutStatus.DISPUTE.name(), previousStatus.name(),
            response, userRepository.findById(currentUserDetails.getUserId()).orElse(null)
        );
        activity.setDisputeStage(DisputeStage.REJECTED);
        activity.setPreviousStatus(ClientPayoutStatus.DISPUTE.name());
        activity.setNewStatus(previousStatus.name());
        activityRepository.save(activity);

        return savedPayout;
    }
    
    // ==================== QUERY METHODS ====================

    @Transactional(readOnly = true)
    public List<ClientPayout> getClientPayoutsByRole() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        Long userId = currentUserDetails.getUserId();
        
        switch (userRole.toUpperCase()) {
            case "ADMIN":
                return clientPayoutRepository.findAllByOrderByCreatedAtDesc();
            case "MANAGER":
            case "BRANCH_PARTNER":
            case "ADMINISTRATIVE_ASSISTANT":
                return clientPayoutRepository.findAllByOrderByCreatedAtDesc();
            case "REFERRAL":
            case "COMPANY":
                return clientPayoutRepository.findByUserIdOrderByCreatedAtDesc(userId);
            default:
                throw new BusinessException("Access denied for role: " + userRole);
        }
    }
    
    @Transactional(readOnly = true)
    public ClientPayout getClientPayoutById(Long payoutId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        Long userId = currentUserDetails.getUserId();
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate access
        validatePayoutAccess(payout, userRole, userId);
        
        return payout;
    }
    
    @Transactional(readOnly = true)
    public List<ClientPayoutActivity> getPayoutActivities(Long payoutId) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        Long userId = currentUserDetails.getUserId();
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found with ID: " + payoutId));
        
        // Validate access
        validatePayoutAccess(payout, userRole, userId);
        
        return activityRepository.findByClientPayoutIdOrderByDoneAtDesc(payoutId);
    }
    
    // ==================== HELPER METHODS ====================

    private void validateUserRoleMatchesSourceType(User user, SourceType sourceType) {
        String userRole = user.getRole().getName();
        
        if (sourceType.equals(SourceType.REFERRAL) && !userRole.equals("REFERRAL")) {
            throw new BusinessException("User must have REFERRAL role for REFERRAL source type");
        }
        
        if (sourceType.equals(SourceType.COMPANY) && !userRole.equals("COMPANY")) {
            throw new BusinessException("User must have COMPANY role for COMPANY source type");
        }
    }
    
    private void validateAmountAssignmentAccess(String userRole) {
        if (!List.of("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT").contains(userRole.toUpperCase())) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can assign amounts.");
        }
    }
    
    private void validatePaymentProcessingAccess(String userRole) {
        if (!List.of("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT").contains(userRole.toUpperCase())) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can process payments.");
        }
    }
    
    private void validateDisputeInitiationAccess(String userRole) {
        if (!List.of("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT").contains(userRole.toUpperCase())) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can initiate disputes.");
        }
    }
    
    private void validateDisputeResponseAccess(Long payoutId, String userRole, Long userId) {
        if (!List.of("REFERRAL", "COMPANY").contains(userRole.toUpperCase())) {
            throw new BusinessException("Access denied. Only REFERRAL or COMPANY can respond to disputes.");
        }
        
        ClientPayout payout = clientPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Client payout not found"));
        
        if (!payout.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied. You can only respond to your own payout disputes.");
        }
    }
    
    private void validatePayoutAccess(ClientPayout payout, String userRole, Long userId) {
        switch (userRole.toUpperCase()) {
            case "ADMIN":
            case "MANAGER":
            case "BRANCH_PARTNER":
            case "ADMINISTRATIVE_ASSISTANT":
                return; // Full access
            case "REFERRAL":
            case "COMPANY":
                if (!payout.getUser().getId().equals(userId)) {
                    throw new BusinessException("Access denied. You can only view your own payouts.");
                }
                break;
            default:
                throw new BusinessException("Access denied for role: " + userRole);
        }
    }
    
    private boolean canInitiateDispute(ClientPayoutStatus status) {
        return List.of(ClientPayoutStatus.PENDING, ClientPayoutStatus.AMOUNT_ASSIGNED, 
                     ClientPayoutStatus.PARTIAL_PAID).contains(status);
    }
    
    private ClientPayoutStatus determinePreviousStatus(ClientPayout payout) {
        if (payout.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            return payout.isFullyPaid() ? ClientPayoutStatus.PAID : ClientPayoutStatus.PARTIAL_PAID;
        } else if (payout.getAssignedAmount() != null) {
            return ClientPayoutStatus.AMOUNT_ASSIGNED;
        } else {
            return ClientPayoutStatus.PENDING;
        }
    }
    
    public void logActivity(ClientPayout payout, ClientPayoutAction action,
                          String oldValue, String newValue, String reason, Long userId) {
        // Tamper-evident audit — must succeed; failure rolls back the enclosing transaction.
        financialAuditService.record(
                toFinancialAction(action),
                "ClientPayout",
                payout.getId(),
                userId,
                oldValue,
                newValue,
                reason);

        // Legacy activity log — kept for operational detail (dispute stage, status strings, etc.).
        ClientPayoutActivity activity = new ClientPayoutActivity(
            payout, action, oldValue, newValue, reason,
            userRepository.findById(userId).orElse(null)
        );
        activityRepository.save(activity);
    }

    private FinancialAuditAction toFinancialAction(ClientPayoutAction action) {
        return switch (action) {
            case CREATED          -> FinancialAuditAction.PAYOUT_CREATED;
            case AMOUNT_ASSIGNED  -> FinancialAuditAction.PAYOUT_AMOUNT_ASSIGNED;
            case PAYMENT_ADDED    -> FinancialAuditAction.PAYOUT_PAYMENT_ADDED;
            case DISPUTE_INITIATED -> FinancialAuditAction.PAYOUT_DISPUTE_INITIATED;
            case DISPUTE_ACCEPTED  -> FinancialAuditAction.PAYOUT_DISPUTE_ACCEPTED;
            case DISPUTE_REJECTED  -> FinancialAuditAction.PAYOUT_DISPUTE_REJECTED;
            case STATUS_CHANGED    -> FinancialAuditAction.PAYOUT_STATUS_CHANGED;
        };
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
