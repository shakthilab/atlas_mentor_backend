package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.PaymentTransaction;
import com.lab.atlasmentor.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<PaymentTransaction> findByStudentPaymentIdOrderByCreatedAtDesc(Long paymentId);
    
    List<PaymentTransaction> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    List<PaymentTransaction> findByPaymentMethodOrderByCreatedAtDesc(PaymentMethod paymentMethod);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.student.id = :studentId AND pt.isDeleted = false ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findActiveByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.studentPayment.id = :paymentId AND pt.isDeleted = false ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findActiveByPaymentIdOrderByCreatedAtDesc(@Param("paymentId") Long paymentId);
    
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.studentPayment.id = :paymentId AND pt.isDeleted = false")
    BigDecimal sumAmountByPaymentId(@Param("paymentId") Long paymentId);
    
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.student.id = :studentId AND pt.isDeleted = false")
    BigDecimal sumAmountByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.student.id = :studentId AND pt.isDeleted = false")
    Long countActiveTransactionsByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.createdAt BETWEEN :startDate AND :endDate AND pt.isDeleted = false ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByDateRangeOrderByCreatedAtDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.student.id = :studentId AND pt.createdAt BETWEEN :startDate AND :endDate AND pt.isDeleted = false ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByStudentIdAndDateRangeOrderByCreatedAtDesc(@Param("studentId") Long studentId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.transactionReference = :reference AND pt.isDeleted = false")
    Optional<PaymentTransaction> findByTransactionReference(@Param("reference") String reference);
}
