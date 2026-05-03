package com.lab.atlasmentor.model;

import com.lab.atlasmentor.enums.PaymentMethod;
import com.lab.atlasmentor.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentTransaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, referencedColumnName = "id")
    private StudentPayment studentPayment;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType = TransactionType.CREDIT;
    
    @Column(name = "transaction_reference", length = 200)
    private String transactionReference;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    public PaymentTransaction() {}
    
    public PaymentTransaction(Student student, StudentPayment studentPayment, BigDecimal amount, 
                            PaymentMethod paymentMethod, TransactionType transactionType, 
                            String transactionReference, Long createdBy) {
        this.student = student;
        this.studentPayment = studentPayment;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.transactionReference = transactionReference;
        this.createdBy = createdBy;
    }
    
    /**
     * Soft delete the transaction
     */
    public void softDelete() {
        this.isDeleted = true;
    }
    
    /**
     * Restore the transaction
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * Check if transaction is deleted
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
