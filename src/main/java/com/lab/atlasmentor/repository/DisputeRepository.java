package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Dispute;
import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.DisputePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    List<Dispute> findByStudentIdOrderByRaisedAtDesc(Long studentId);
    
    List<Dispute> findByRaisedByOrderByRaisedAtDesc(Long raisedBy);
    
    List<Dispute> findByResolvedByOrderByResolvedAtDesc(Long resolvedBy);
    
    List<Dispute> findByStatusOrderByRaisedAtDesc(DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.student.id = :studentId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByStudentIdOrderByRaisedAtDesc(@Param("studentId") Long studentId);
    
    @Query("SELECT d FROM Dispute d WHERE d.raisedBy = :raisedBy AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByRaisedByOrderByRaisedAtDesc(@Param("raisedBy") Long raisedBy);
    
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByStatusOrderByRaisedAtDesc(@Param("status") DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.relatedApproval.id = :approvalId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByApprovalIdOrderByRaisedAtDesc(@Param("approvalId") Long approvalId);
    
    @Query("SELECT d FROM Dispute d WHERE d.status IN :statuses AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByStatusesOrderByRaisedAtDesc(@Param("statuses") List<DisputeStatus> statuses);
    
    @Query("SELECT d FROM Dispute d WHERE d.raisedAt BETWEEN :startDate AND :endDate AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByDateRangeOrderByRaisedAtDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = :status AND d.isDeleted = false")
    Long countActiveByStatus(@Param("status") DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.student.id = :studentId AND d.status = :status AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findActiveByStudentIdAndStatusOrderByRaisedAtDesc(@Param("studentId") Long studentId, @Param("status") DisputeStatus status);
    
    // Additional methods for DisputeService
    @Query("SELECT d FROM Dispute d WHERE d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByIsDeletedFalseOrderByRaisedAtDesc();
    
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByStatusAndIsDeletedFalse(@Param("status") DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.priority = :priority AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByPriorityAndIsDeletedFalse(@Param("priority") DisputePriority priority);
    
    @Query("SELECT d FROM Dispute d WHERE d.student.id = :studentId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByStudentIdAndIsDeletedFalse(@Param("studentId") Long studentId);
    
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.priority = :priority AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByStatusAndPriorityAndIsDeletedFalse(@Param("status") DisputeStatus status, @Param("priority") DisputePriority priority);
    
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.priority = :priority AND d.student.id = :studentId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByStatusAndPriorityAndStudentIdAndIsDeletedFalse(@Param("status") DisputeStatus status, @Param("priority") DisputePriority priority, @Param("studentId") Long studentId);
    
    @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.student.id = :studentId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByStatusAndStudentIdAndIsDeletedFalse(@Param("status") DisputeStatus status, @Param("studentId") Long studentId);
    
    @Query("SELECT d FROM Dispute d WHERE d.priority = :priority AND d.student.id = :studentId AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByPriorityAndStudentIdAndIsDeletedFalse(@Param("priority") DisputePriority priority, @Param("studentId") Long studentId);
    
    @Query("SELECT d FROM Dispute d WHERE d.raisedBy = :raisedBy AND d.status = :status AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByRaisedByAndStatusAndIsDeletedFalse(@Param("raisedBy") Long raisedBy, @Param("status") DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.raisedBy = :raisedBy AND d.isDeleted = false ORDER BY d.raisedAt DESC")
    List<Dispute> findByRaisedByAndIsDeletedFalse(@Param("raisedBy") Long raisedBy);
}
