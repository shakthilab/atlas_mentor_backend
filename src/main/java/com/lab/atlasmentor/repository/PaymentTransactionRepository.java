package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.PaymentTransaction;
import com.lab.atlasmentor.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("DELETE FROM PaymentTransaction pt WHERE pt.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT payment_method, COUNT(*) as cnt, COALESCE(SUM(amount), 0) as total_amount " +
           "FROM payment_transactions WHERE is_deleted = false AND created_at >= :from " +
           "GROUP BY payment_method ORDER BY total_amount DESC", nativeQuery = true)
    List<Object[]> getPaymentMethodDistribution(@Param("from") java.time.LocalDateTime from);

    @Query(value = "SELECT pt.payment_method, COUNT(*) as cnt, COALESCE(SUM(pt.amount), 0) as total_amount " +
           "FROM payment_transactions pt " +
           "JOIN student_payments sp ON pt.payment_id = sp.id " +
           "WHERE pt.is_deleted = false AND sp.branch_id = :branchId AND pt.created_at >= :from " +
           "GROUP BY pt.payment_method ORDER BY total_amount DESC", nativeQuery = true)
    List<Object[]> getPaymentMethodDistributionForBranch(@Param("branchId") Long branchId, @Param("from") java.time.LocalDateTime from);
}
