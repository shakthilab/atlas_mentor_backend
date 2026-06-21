package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import com.lab.atlasmentor.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentPaymentRepository extends JpaRepository<StudentPayment, Long> {
    
    Optional<StudentPayment> findByStudentId(Long studentId);
    
    List<StudentPayment> findBySourceIdOrderByCreatedAtDesc(Long sourceId);
    
    List<StudentPayment> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(SourceType sourceType, Long sourceId);
    
    List<StudentPayment> findByPaymentStatusOrderByCreatedAtDesc(StudentPaymentStatus paymentStatus);
    
    List<StudentPayment> findByStudentStatusOrderByCreatedAtDesc(StudentStatusEnhanced studentStatus);
    
    List<StudentPayment> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.student.id = :studentId AND sp.sourceType = :sourceType AND sp.isDeleted = false")
    Optional<StudentPayment> findByStudentIdAndSourceType(@Param("studentId") Long studentId, @Param("sourceType") SourceType sourceType);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.sourceId = :sourceId AND sp.sourceType = :sourceType AND sp.isDeleted = false")
    List<StudentPayment> findBySourceIdAndSourceType(@Param("sourceId") Long sourceId, @Param("sourceType") SourceType sourceType);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.branchId = :branchId AND sp.paymentStatus = :paymentStatus ORDER BY sp.createdAt DESC")
    List<StudentPayment> findByBranchIdAndPaymentStatusOrderByCreatedAtDesc(@Param("branchId") Long branchId, @Param("paymentStatus") StudentPaymentStatus paymentStatus);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.branchId = :branchId AND sp.studentStatus = :studentStatus ORDER BY sp.createdAt DESC")
    List<StudentPayment> findByBranchIdAndStudentStatusOrderByCreatedAtDesc(@Param("branchId") Long branchId, @Param("studentStatus") StudentStatusEnhanced studentStatus);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.isAmountLocked = false AND sp.isDeleted = false")
    List<StudentPayment> findByAmountLockedFalse();
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.student.id = :studentId AND sp.isAmountLocked = true AND sp.isDeleted = false")
    Optional<StudentPayment> findByStudentIdAndAmountLockedTrue(@Param("studentId") Long studentId);
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.assignedAmount IS NULL AND sp.isDeleted = false")
    List<StudentPayment> findByAssignedAmountNull();
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.isDeleted = false ORDER BY sp.createdAt DESC")
    List<StudentPayment> findAllActiveOrderByCreatedAtDesc();
    
    @Query("SELECT sp FROM StudentPayment sp WHERE sp.student.id = :studentId AND sp.isDeleted = false")
    Optional<StudentPayment> findActiveByStudentId(@Param("studentId") Long studentId);
    
    @Modifying
    @Query("DELETE FROM StudentPayment sp WHERE sp.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT COALESCE(SUM(assigned_amount), 0) as total_billed, COALESCE(SUM(paid_amount), 0) as total_paid " +
           "FROM student_payments WHERE is_deleted = false AND assigned_amount IS NOT NULL AND created_at >= :from", nativeQuery = true)
    List<Object[]> getFinancialSummary(@Param("from") java.time.LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'Mon') as month, " +
           "payment_status, " +
           "COALESCE(SUM(CASE WHEN payment_status IN ('PAID','PARTIAL') THEN paid_amount " +
           "ELSE assigned_amount END), 0) as amount " +
           "FROM student_payments WHERE is_deleted = false AND assigned_amount IS NOT NULL " +
           "AND created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', created_at), payment_status " +
           "ORDER BY DATE_TRUNC('month', created_at)", nativeQuery = true)
    List<Object[]> getMonthlyPaymentBreakdown(@Param("from") java.time.LocalDateTime from);

    // ==================== BRANCH-SCOPED DASHBOARD QUERIES ====================

    @Query(value = "SELECT COALESCE(SUM(assigned_amount), 0) as total_billed, COALESCE(SUM(paid_amount), 0) as total_paid " +
           "FROM student_payments WHERE is_deleted = false AND assigned_amount IS NOT NULL AND branch_id = :branchId AND created_at >= :from", nativeQuery = true)
    List<Object[]> getFinancialSummaryForBranch(@Param("branchId") Long branchId, @Param("from") java.time.LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'Mon') as month, " +
           "payment_status, " +
           "COALESCE(SUM(CASE WHEN payment_status IN ('PAID','PARTIAL') THEN paid_amount " +
           "ELSE assigned_amount END), 0) as amount " +
           "FROM student_payments WHERE is_deleted = false AND assigned_amount IS NOT NULL AND branch_id = :branchId " +
           "AND created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', created_at), payment_status " +
           "ORDER BY DATE_TRUNC('month', created_at)", nativeQuery = true)
    List<Object[]> getMonthlyPaymentBreakdownForBranch(@Param("branchId") Long branchId, @Param("from") java.time.LocalDateTime from);
}
