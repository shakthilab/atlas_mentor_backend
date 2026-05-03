package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.SourceType;
import com.lab.atlasmentor.enums.ApprovalStatus;
import com.lab.atlasmentor.enums.DisputeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentWithStudentPaymentDto {
    
    private Long studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private StudentStatus status;
    private String courseName;
    private String intakePeriod;
    private String notes;
    private String branchName;
    private LocalDateTime createdAt;
    
    // StudentPayment details
    private Long paymentId;
    private BigDecimal assignedAmount;
    private BigDecimal paidAmount;
    private StudentPaymentStatus paymentStatus;
    private SourceType sourceType;
    private Long sourceId;
    private Boolean isAmountLocked;
    private Long branchId;
    private String paymentNotes;
    private LocalDateTime paymentCreatedAt;
    private ApprovalStatus approvalStatus;
    
    // Dispute information
    private DisputeStatus disputeStatus;
    
    public StudentWithStudentPaymentDto() {}
    
    public StudentWithStudentPaymentDto(Long studentId, String firstName, String lastName, String email, String phone, 
                               StudentStatus status, String courseName, String intakePeriod, String notes, 
                               String branchName, LocalDateTime createdAt, Long paymentId, BigDecimal assignedAmount,
                               BigDecimal paidAmount, StudentPaymentStatus paymentStatus, SourceType sourceType,
                               Long sourceId, Boolean isAmountLocked, Long branchId, String paymentNotes,
                               LocalDateTime paymentCreatedAt, ApprovalStatus approvalStatus, DisputeStatus disputeStatus) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.courseName = courseName;
        this.intakePeriod = intakePeriod;
        this.notes = notes;
        this.branchName = branchName;
        this.createdAt = createdAt;
        this.paymentId = paymentId;
        this.assignedAmount = assignedAmount;
        this.paidAmount = paidAmount;
        this.paymentStatus = paymentStatus;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.isAmountLocked = isAmountLocked;
        this.branchId = branchId;
        this.paymentNotes = paymentNotes;
        this.paymentCreatedAt = paymentCreatedAt;
        this.approvalStatus = approvalStatus;
        this.disputeStatus = disputeStatus;
    }
}
