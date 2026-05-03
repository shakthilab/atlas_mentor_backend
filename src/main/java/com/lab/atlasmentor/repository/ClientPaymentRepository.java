package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ClientPayment;
import com.lab.atlasmentor.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientPaymentRepository extends JpaRepository<ClientPayment, Long> {
    
    Optional<ClientPayment> findByStudentId(Long studentId);
    
    List<ClientPayment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<ClientPayment> findByReferralIdOrderByCreatedAtDesc(Long referralId);
    
    List<ClientPayment> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    
    List<ClientPayment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    @Query("SELECT p FROM ClientPayment p WHERE p.studentId = :studentId AND p.status = :status")
    List<ClientPayment> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM ClientPayment p WHERE p.referralId = :referralId AND p.status = :status")
    List<ClientPayment> findByReferralIdAndStatus(@Param("referralId") Long referralId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM ClientPayment p WHERE p.companyId = :companyId AND p.status = :status")
    List<ClientPayment> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM ClientPayment p WHERE p.branchId = :branchId ORDER BY p.createdAt DESC")
    List<ClientPayment> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId);
    
    @Query("SELECT p FROM ClientPayment p WHERE p.branchId = :branchId AND p.status = :status ORDER BY p.createdAt DESC")
    List<ClientPayment> findByBranchIdAndStatusOrderByCreatedAtDesc(@Param("branchId") Long branchId, @Param("status") PaymentStatus status);
}
