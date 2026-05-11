package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import com.lab.atlasmentor.enums.SourceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "student_payments",
       indexes = {
           @Index(name = "idx_student_payments_student_id", columnList = "student_id"),
           @Index(name = "idx_student_payments_status", columnList = "payment_status"),
           @Index(name = "idx_student_payments_branch_id", columnList = "branch_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentPayment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0")
    private Long version = 0L;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
    
    @Column(name = "source_id", nullable = false)
    private Long sourceId;
    
    @Column(name = "assigned_amount", precision = 10, scale = 2)
    private BigDecimal assignedAmount;
    
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private StudentPaymentStatus paymentStatus = StudentPaymentStatus.NOT_APPLICABLE;
    
    @Column(name = "is_amount_locked", nullable = false)
    private Boolean isAmountLocked = false;
    
    @Column(name = "branch_id")
    private Long branchId;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "student_status", nullable = false)
    private StudentStatusEnhanced studentStatus = StudentStatusEnhanced.ACTIVE;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @OneToMany(mappedBy = "studentPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"studentPayment"})
    private List<PaymentTransaction> transactions;
    
    public StudentPayment() {}
    
    public StudentPayment(Student student, SourceType sourceType, Long sourceId) {
        this.student = student;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }
    
    /**
     * Update payment status based on transactions
     */
    public void updatePaymentStatus() {
        BigDecimal totalPaid = calculateTotalPaid();
        
        if (assignedAmount == null) {
            // No amount assigned yet, keep as NOT_APPLICABLE
            this.paymentStatus = StudentPaymentStatus.NOT_APPLICABLE;
            return;
        }
        
        // Amount is assigned, status should be at least PENDING
        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = StudentPaymentStatus.PENDING;
        } else if (totalPaid.compareTo(assignedAmount) < 0) {
            this.paymentStatus = StudentPaymentStatus.PARTIAL;
        } else if (totalPaid.compareTo(assignedAmount) >= 0) {
            this.paymentStatus = StudentPaymentStatus.PAID;
        }
        
        // Update paid_amount for backward compatibility (read-only)
        this.paidAmount = totalPaid;
    }
    
    /**
     * Calculate total paid amount from transactions
     */
    public BigDecimal calculateTotalPaid() {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return transactions.stream()
                .filter(transaction -> !transaction.isDeleted())
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if payment is fully paid
     */
    public boolean isFullyPaid() {
        BigDecimal totalPaid = calculateTotalPaid();
        return assignedAmount != null && totalPaid.compareTo(assignedAmount) >= 0;
    }
    
    /**
     * Check if payment is pending (no amount paid)
     */
    public boolean isPending() {
        BigDecimal totalPaid = calculateTotalPaid();
        return totalPaid.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if payment is partially paid
     */
    public boolean isPartiallyPaid() {
        BigDecimal totalPaid = calculateTotalPaid();
        return assignedAmount != null && 
               totalPaid.compareTo(BigDecimal.ZERO) > 0 && 
               totalPaid.compareTo(assignedAmount) < 0;
    }
    
    /**
     * Soft delete the payment record
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the payment record
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if payment is deleted
     */
    public boolean isPaymentDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
