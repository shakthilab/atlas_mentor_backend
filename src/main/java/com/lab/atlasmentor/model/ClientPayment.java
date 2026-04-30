package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import com.lab.atlasmentor.enums.PaymentStatus;

@Entity
@Table(name = "payments")
@Data
public class ClientPayment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "referral_id")
    private Long referralId;
    
    @Column(name = "company_id")
    private Long companyId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "approved_by")
    private Long approvedBy;
    
    @Column(name = "branch_id")
    private Long branchId;
    
    public ClientPayment() {}
    
    public ClientPayment(Long studentId, BigDecimal amount) {
        this.studentId = studentId;
        this.amount = amount;
    }
    
}
