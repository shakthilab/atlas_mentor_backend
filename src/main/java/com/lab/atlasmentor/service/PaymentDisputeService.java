package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.StudentPaymentDto;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.repository.StudentPaymentRepository;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentDisputeService {

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Transactional
    public StudentPaymentDto markPaymentAsDisputed(Long paymentId, String disputeReason, Long adminUserId) {
        StudentPayment payment = studentPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (payment.getPaymentStatus() != StudentPaymentStatus.PENDING) {
            throw new RuntimeException("Only PENDING payments can be marked as disputed");
        }

        // Mark payment as DISPUTE
        payment.setPaymentStatus(StudentPaymentStatus.DISPUTE);
        
        // Add dispute reason to notes
        String existingNotes = payment.getNotes() != null ? payment.getNotes() : "";
        String updatedNotes = existingNotes.isEmpty() ? 
            "DISPUTE: " + disputeReason : 
            existingNotes + "\nDISPUTE: " + disputeReason;
        
        payment.setNotes(updatedNotes);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(adminUserId);

        StudentPayment savedPayment = studentPaymentRepository.save(payment);
        return convertToDto(savedPayment);
    }

    @Transactional
    public StudentPaymentDto acceptPaymentDispute(Long paymentId, String response, Long userId) {
        StudentPayment payment = studentPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (payment.getPaymentStatus() != StudentPaymentStatus.DISPUTE) {
            throw new RuntimeException("Only DISPUTE payments can be accepted");
        }

        // Accept dispute - set status to REJECTED
        payment.setPaymentStatus(StudentPaymentStatus.REJECTED);
        
        // Add acceptance response to notes
        String existingNotes = payment.getNotes() != null ? payment.getNotes() : "";
        String updatedNotes = existingNotes + "\nDISPUTE ACCEPTED: " + response;
        
        payment.setNotes(updatedNotes);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(userId);

        StudentPayment savedPayment = studentPaymentRepository.save(payment);
        return convertToDto(savedPayment);
    }

    @Transactional
    public StudentPaymentDto rejectPaymentDispute(Long paymentId, String response, Long userId) {
        StudentPayment payment = studentPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (payment.getPaymentStatus() != StudentPaymentStatus.DISPUTE) {
            throw new RuntimeException("Only DISPUTE payments can be rejected");
        }

        // Reject dispute - keep status as DISPUTE but add comments
        String existingNotes = payment.getNotes() != null ? payment.getNotes() : "";
        String updatedNotes = existingNotes + "\nDISPUTE REJECTED: " + response;
        
        payment.setNotes(updatedNotes);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(userId);

        StudentPayment savedPayment = studentPaymentRepository.save(payment);
        return convertToDto(savedPayment);
    }

    private StudentPaymentDto convertToDto(StudentPayment payment) {
        StudentPaymentDto dto = new StudentPaymentDto();
        dto.setId(payment.getId());
        dto.setStudentId(payment.getStudent().getId());
        dto.setAssignedAmount(payment.getAssignedAmount());
        dto.setPaidAmount(payment.getPaidAmount());
        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setNotes(payment.getNotes());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}
