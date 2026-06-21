package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.StudentStatusApproval;
import com.lab.atlasmentor.enums.ApprovalStatus;
import com.lab.atlasmentor.enums.StudentStatusEnhanced;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentStatusApprovalRepository extends JpaRepository<StudentStatusApproval, Long> {
    
    List<StudentStatusApproval> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<StudentStatusApproval> findByRequestedByOrderByCreatedAtDesc(Long requestedBy);
    
    List<StudentStatusApproval> findByApprovedByOrderByCreatedAtDesc(Long approvedBy);
    
    List<StudentStatusApproval> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);
    
    List<StudentStatusApproval> findByRequestedStatusOrderByCreatedAtDesc(StudentStatusEnhanced requestedStatus);
    
    @Query("SELECT ssa FROM StudentStatusApproval ssa WHERE ssa.student.id = :studentId AND ssa.status = :status ORDER BY ssa.createdAt DESC")
    List<StudentStatusApproval> findByStudentIdAndStatusOrderByCreatedAtDesc(@Param("studentId") Long studentId, @Param("status") ApprovalStatus status);
    
    @Query("SELECT ssa FROM StudentStatusApproval ssa WHERE ssa.requestedBy = :requestedBy AND ssa.status = :status ORDER BY ssa.createdAt DESC")
    List<StudentStatusApproval> findByRequestedByAndStatusOrderByCreatedAtDesc(@Param("requestedBy") Long requestedBy, @Param("status") ApprovalStatus status);
    
    @Query("SELECT ssa FROM StudentStatusApproval ssa WHERE ssa.student.id = :studentId AND ssa.requestedStatus = :requestedStatus AND ssa.status = 'PENDING'")
    Optional<StudentStatusApproval> findPendingByStudentIdAndRequestedStatus(@Param("studentId") Long studentId, @Param("requestedStatus") StudentStatusEnhanced requestedStatus);
    
    @Query("SELECT ssa FROM StudentStatusApproval ssa WHERE ssa.student.id = :studentId AND ssa.status = 'PENDING'")
    List<StudentStatusApproval> findPendingByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT ssa FROM StudentStatusApproval ssa WHERE ssa.requestedStatus = :requestedStatus AND ssa.status = 'PENDING'")
    List<StudentStatusApproval> findPendingByRequestedStatus(@Param("requestedStatus") StudentStatusEnhanced requestedStatus);
    
    @Query("SELECT COUNT(ssa) FROM StudentStatusApproval ssa WHERE ssa.student.id = :studentId AND ssa.status = 'PENDING'")
    Long countPendingByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ssa) FROM StudentStatusApproval ssa WHERE ssa.status = 'PENDING'")
    Long countAllPending();
}
