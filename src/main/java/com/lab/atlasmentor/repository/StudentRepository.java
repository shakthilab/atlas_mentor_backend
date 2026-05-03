package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.dto.StudentWithStudentPaymentDto;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.assignedBy.id = :counsellorId")
    long countStudentsByAssignedBy(@Param("counsellorId") Long counsellorId);

    @Query("SELECT s FROM Student s LEFT JOIN s.user u " +
           "WHERE (:status IS NULL OR s.status = :status) " +
           "AND (LOWER(u.firstName) LIKE :search " +
           "OR LOWER(u.lastName) LIKE :search " +
           "OR LOWER(s.email) LIKE :search " +
           "OR s.phone LIKE :search " +
           "OR u IS NULL)")
    Page<Student> findByFilters(@Param("status") StudentStatus status, @Param("search") String search, Pageable pageable);
    
    // Branch-based access control methods
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId)", nativeQuery = true)
    Page<Student> findAllWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND (:status IS NULL OR s.status = :status)", nativeQuery = true)
    Page<Student> findByStatusWithAccess(@Param("status") String status, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT COUNT(s) FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId)", nativeQuery = true)
    long countWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersWithAccess(@Param("status") String status, @Param("search") String search, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.assignedBy_id = :counsellorId AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersForCounsellor(@Param("status") String status, @Param("search") String search, @Param("counsellorId") Long counsellorId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.created_by = :creatorId AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersForCreator(@Param("status") String status, @Param("search") String search, @Param("creatorId") Long creatorId, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.branch.id = :branchId")
    Long countStudentsByBranchId(@Param("branchId") Long branchId);
    
    // Methods for finding non-registered students (status != REGISTERED)
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND s.status != 'REGISTERED' AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusWithAccess(@Param("search") String search, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.assignedBy_id = :counsellorId AND s.status != 'REGISTERED' AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusForCounsellor(@Param("search") String search, @Param("counsellorId") Long counsellorId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.created_by = :creatorId AND s.status != 'REGISTERED' AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusForCreator(@Param("search") String search, @Param("creatorId") Long creatorId, Pageable pageable);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, s.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END, " +
           "null) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy IN " +
           "(SELECT u.id FROM User u JOIN u.role r WHERE r.name IN ('REFERRAL', 'COMPANY')) " +
           "OR p.sourceType IN ('REFERRAL', 'COMPANY'))")
    List<StudentWithStudentPaymentDto> findStudentsWithPaymentByReferralAndCompany();
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, s.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END, " +
           "null) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy = :referralId " +
           "OR (p.sourceType = 'REFERRAL' AND p.sourceId = :referralId))")
    List<StudentWithStudentPaymentDto> findStudentsWithPaymentByReferral(@Param("referralId") Long referralId);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, s.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END, " +
           "null) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy = :companyId " +
           "OR (p.sourceType = 'COMPANY' AND p.sourceId = :companyId))")
    List<StudentWithStudentPaymentDto> findStudentsWithPaymentByCompany(@Param("companyId") Long companyId);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, s.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END, " +
           "null) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "INNER JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE s.branch.id = :branchId " +
           "AND p.isDeleted = false " +
           "AND (s.createdBy IN " +
           "(SELECT u.id FROM User u JOIN u.role r WHERE r.name IN ('REFERRAL', 'COMPANY')) " +
           "OR p.sourceType IN ('REFERRAL', 'COMPANY'))")
    List<StudentWithStudentPaymentDto> findStudentsWithPaymentByBranch(@Param("branchId") Long branchId);
}




