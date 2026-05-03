package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.PaymentStatus;
import com.lab.atlasmentor.enums.StudentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentWithPaymentDto {
    
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
    
    // Payment details
    private Long paymentId;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private Long referralId;
    private Long companyId;
    private LocalDateTime paymentCreatedAt;
    private Long approvedBy;
    
    public StudentWithPaymentDto() {}
    
    public StudentWithPaymentDto(Long studentId, String firstName, String lastName, String email, String phone, 
                               StudentStatus status, String courseName, String intakePeriod, String notes, 
                               String branchName, LocalDateTime createdAt, Long paymentId, BigDecimal amount, 
                               PaymentStatus paymentStatus, Long referralId, Long companyId, 
                               LocalDateTime paymentCreatedAt, Long approvedBy) {
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
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.referralId = referralId;
        this.companyId = companyId;
        this.paymentCreatedAt = paymentCreatedAt;
        this.approvedBy = approvedBy;
    }
}
