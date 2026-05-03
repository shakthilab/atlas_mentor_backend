package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.DisputeDto;
import com.lab.atlasmentor.dto.DisputeRequest;
import com.lab.atlasmentor.model.Dispute;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.StudentStatusApproval;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.DisputeRepository;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.StudentStatusApprovalRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.StudentPaymentRepository;
import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.DisputePriority;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentStatusApprovalRepository studentStatusApprovalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Transactional
    public DisputeDto createDispute(DisputeRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + request.getStudentId()));

        StudentStatusApproval relatedApproval = null;
        if (request.getRelatedApprovalId() != null) {
            relatedApproval = studentStatusApprovalRepository.findById(request.getRelatedApprovalId())
                .orElseThrow(() -> new RuntimeException("Related approval not found with id: " + request.getRelatedApprovalId()));
        }

        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        Dispute dispute = new Dispute(student, relatedApproval, currentUser.getUserId(), request.getDisputeReason());
        dispute.setPriority(request.getPriority());
        
        Dispute savedDispute = disputeRepository.save(dispute);
        return convertToDto(savedDispute);
    }

    @Transactional(readOnly = true)
    public DisputeDto getDisputeById(Long id) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Dispute has been deleted");
        }
        
        return convertToDto(dispute);
    }

    @Transactional(readOnly = true)
    public List<DisputeDto> getAllDisputes(DisputeStatus status, DisputePriority priority, Long studentId) {
        List<Dispute> disputes;
        
        if (status != null && priority != null && studentId != null) {
            disputes = disputeRepository.findByStatusAndPriorityAndStudentIdAndIsDeletedFalse(status, priority, studentId);
        } else if (status != null && priority != null) {
            disputes = disputeRepository.findByStatusAndPriorityAndIsDeletedFalse(status, priority);
        } else if (status != null && studentId != null) {
            disputes = disputeRepository.findByStatusAndStudentIdAndIsDeletedFalse(status, studentId);
        } else if (priority != null && studentId != null) {
            disputes = disputeRepository.findByPriorityAndStudentIdAndIsDeletedFalse(priority, studentId);
        } else if (status != null) {
            disputes = disputeRepository.findByStatusAndIsDeletedFalse(status);
        } else if (priority != null) {
            disputes = disputeRepository.findByPriorityAndIsDeletedFalse(priority);
        } else if (studentId != null) {
            disputes = disputeRepository.findByStudentIdAndIsDeletedFalse(studentId);
        } else {
            disputes = disputeRepository.findByIsDeletedFalseOrderByRaisedAtDesc();
        }
        
        return disputes.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DisputeDto> getMyDisputes(DisputeStatus status) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        List<Dispute> disputes;
        
        if (status != null) {
            disputes = disputeRepository.findByRaisedByAndStatusAndIsDeletedFalse(currentUser.getUserId(), status);
        } else {
            disputes = disputeRepository.findByRaisedByAndIsDeletedFalse(currentUser.getUserId());
        }
        
        return disputes.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public DisputeDto updateDisputeStatus(Long id, DisputeStatus status) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Cannot update status of deleted dispute");
        }
        
        dispute.setStatus(status);
        Dispute updatedDispute = disputeRepository.save(dispute);
        return convertToDto(updatedDispute);
    }

    @Transactional
    public DisputeDto resolveDispute(Long id, String resolutionNotes, DisputeStatus resolutionStatus) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Cannot resolve deleted dispute");
        }
        
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        if (resolutionStatus == DisputeStatus.RESOLVED) {
            dispute.resolve(currentUser.getUserId(), resolutionNotes);
        } else if (resolutionStatus == DisputeStatus.CLOSED) {
            dispute.close(currentUser.getUserId(), resolutionNotes);
        } else {
            throw new RuntimeException("Invalid resolution status. Must be RESOLVED or CLOSED");
        }
        
        Dispute updatedDispute = disputeRepository.save(dispute);
        return convertToDto(updatedDispute);
    }

    @Transactional
    public DisputeDto closeDispute(Long id, String resolutionNotes) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Cannot close deleted dispute");
        }
        
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        dispute.close(currentUser.getUserId(), resolutionNotes);
        
        Dispute updatedDispute = disputeRepository.save(dispute);
        return convertToDto(updatedDispute);
    }

    @Transactional
    public DisputeDto acceptDispute(Long id, String acceptanceNotes) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Cannot accept deleted dispute");
        }
        
        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot accept dispute. Dispute must be OPEN or IN_PROGRESS");
        }
        
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Set dispute to IN_PROGRESS if it was OPEN, otherwise keep current status
        if (dispute.getStatus() == DisputeStatus.OPEN) {
            dispute.setStatus(DisputeStatus.IN_PROGRESS);
        }
        
        // Add acceptance notes to resolution notes
        String existingNotes = dispute.getResolutionNotes() != null ? dispute.getResolutionNotes() : "";
        String updatedNotes = existingNotes.isEmpty() ? 
            "Accepted: " + acceptanceNotes : 
            existingNotes + "\nAccepted: " + acceptanceNotes;
        
        dispute.setResolutionNotes(updatedNotes);
        dispute.setResolvedBy(currentUser.getUserId());
        
        // Update student payment status to REJECTED when dispute is accepted
        updateStudentPaymentStatusToRejected(dispute.getStudent().getId());
        
        Dispute updatedDispute = disputeRepository.save(dispute);
        return convertToDto(updatedDispute);
    }
    
    /**
     * Update student payment status to REJECTED when dispute is accepted
     */
    private void updateStudentPaymentStatusToRejected(Long studentId) {
        try {
            // Find active student payment for the student
            StudentPayment studentPayment = studentPaymentRepository.findActiveByStudentId(studentId)
                .orElse(null);
            
            if (studentPayment != null) {
                // Update payment status to REJECTED
                studentPayment.setPaymentStatus(StudentPaymentStatus.REJECTED);
                studentPaymentRepository.save(studentPayment);
            }
        } catch (Exception e) {
            // Log error but don't fail the dispute acceptance
            // In a production environment, you would want to log this properly
            System.err.println("Warning: Failed to update student payment status to REJECTED for student ID: " + studentId);
        }
    }
    
    @Transactional
    public DisputeDto rejectDispute(Long id, String rejectionReason) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        if (dispute.isDisputeDeleted()) {
            throw new RuntimeException("Cannot reject deleted dispute");
        }
        
        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot reject dispute. Dispute must be OPEN or IN_PROGRESS");
        }
        
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Set dispute to CLOSED with rejection
        dispute.setStatus(DisputeStatus.CLOSED);
        
        // Add rejection reason to resolution notes
        String existingNotes = dispute.getResolutionNotes() != null ? dispute.getResolutionNotes() : "";
        String updatedNotes = existingNotes.isEmpty() ? 
            "Rejected: " + rejectionReason : 
            existingNotes + "\nRejected: " + rejectionReason;
        
        dispute.setResolutionNotes(updatedNotes);
        dispute.setResolvedBy(currentUser.getUserId());
        dispute.setResolvedAt(java.time.LocalDateTime.now());
        
        Dispute updatedDispute = disputeRepository.save(dispute);
        return convertToDto(updatedDispute);
    }
    
    @Transactional
    public void deleteDispute(Long id) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dispute not found with id: " + id));
        
        dispute.softDelete();
        disputeRepository.save(dispute);
    }

    private DisputeDto convertToDto(Dispute dispute) {
        String studentName = "Unknown";
        String studentEmail = "Unknown";
        
        if (dispute.getStudent() != null) {
            studentName = dispute.getStudent().getUser() != null ? 
                dispute.getStudent().getUser().getFirstName() + " " + dispute.getStudent().getUser().getLastName() : 
                "Unknown";
            studentEmail = dispute.getStudent().getEmail() != null ? dispute.getStudent().getEmail() : "Unknown";
        }
        
        String raisedByName = getUserFullName(dispute.getRaisedBy());
        String resolvedByName = getUserFullName(dispute.getResolvedBy());
        
        Long relatedApprovalId = dispute.getRelatedApproval() != null ? dispute.getRelatedApproval().getId() : null;
        
        return new DisputeDto(
            dispute.getId(),
            dispute.getStudent() != null ? dispute.getStudent().getId() : null,
            studentName,
            studentEmail,
            relatedApprovalId,
            dispute.getRaisedBy(),
            raisedByName,
            dispute.getResolvedBy(),
            resolvedByName,
            dispute.getStatus(),
            dispute.getPriority(),
            dispute.getDisputeReason(),
            dispute.getResolutionNotes(),
            dispute.getRaisedAt(),
            dispute.getResolvedAt(),
            dispute.getResolutionDeadline(),
            dispute.getCreatedAt(),
            dispute.getUpdatedAt()
        );
    }

    private String getUserFullName(Long userId) {
        if (userId == null) return null;
        
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return user.getFirstName() + " " + user.getLastName();
            }
        } catch (Exception e) {
            // Log error if needed, but return null as fallback
        }
        return null;
    }
}
