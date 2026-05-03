package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.PaymentAmountChange;
import com.lab.atlasmentor.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAmountChangeRepository extends JpaRepository<PaymentAmountChange, Long> {
    
    List<PaymentAmountChange> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<PaymentAmountChange> findByRequestedByOrderByCreatedAtDesc(Long requestedBy);
    
    List<PaymentAmountChange> findByApprovedByOrderByCreatedAtDesc(Long approvedBy);
    
    List<PaymentAmountChange> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);
    
    @Query("SELECT pac FROM PaymentAmountChange pac WHERE pac.student.id = :studentId AND pac.status = :status ORDER BY pac.createdAt DESC")
    List<PaymentAmountChange> findByStudentIdAndStatusOrderByCreatedAtDesc(@Param("studentId") Long studentId, @Param("status") ApprovalStatus status);
    
    @Query("SELECT pac FROM PaymentAmountChange pac WHERE pac.requestedBy = :requestedBy AND pac.status = :status ORDER BY pac.createdAt DESC")
    List<PaymentAmountChange> findByRequestedByAndStatusOrderByCreatedAtDesc(@Param("requestedBy") Long requestedBy, @Param("status") ApprovalStatus status);
    
    @Query("SELECT pac FROM PaymentAmountChange pac WHERE pac.student.id = :studentId AND pac.status = 'PENDING'")
    Optional<PaymentAmountChange> findPendingByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(pac) FROM PaymentAmountChange pac WHERE pac.student.id = :studentId AND pac.status = 'PENDING'")
    Long countPendingByStudentId(@Param("studentId") Long studentId);
}
