package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.PaymentAudit;
import com.lab.atlasmentor.enums.PaymentAuditAction;
import com.lab.atlasmentor.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
    
    List<PaymentAudit> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<PaymentAudit> findByDoneByOrderByCreatedAtDesc(Long doneBy);
    
    List<PaymentAudit> findByActionOrderByCreatedAtDesc(PaymentAuditAction action);
    
    List<PaymentAudit> findByStudentIdAndActionOrderByCreatedAtDesc(Long studentId, PaymentAuditAction action);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.student.id = :studentId AND pa.createdAt BETWEEN :startDate AND :endDate ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findByStudentIdAndDateRangeOrderByCreatedAtDesc(@Param("studentId") Long studentId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.doneBy = :doneBy AND pa.createdAt BETWEEN :startDate AND :endDate ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findByDoneByAndDateRangeOrderByCreatedAtDesc(@Param("doneBy") Long doneBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.action = :action AND pa.createdAt BETWEEN :startDate AND :endDate ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findByActionAndDateRangeOrderByCreatedAtDesc(@Param("action") PaymentAuditAction action, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(pa) FROM PaymentAudit pa WHERE pa.student.id = :studentId AND pa.action = :action AND pa.createdAt BETWEEN :startDate AND :endDate")
    Long countByStudentIdAndActionAndDateRange(@Param("studentId") Long studentId, @Param("action") PaymentAuditAction action, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    void deleteByStudent(Student student);
}
