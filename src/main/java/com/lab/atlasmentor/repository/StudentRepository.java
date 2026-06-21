package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.dto.StudentWithStudentPaymentDto;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT s FROM Student s JOIN s.user u WHERE u.phone = :phone")
    Optional<Student> findByPhone(@Param("phone") String phone);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.assignedBy.id = :counsellorId")
    long countStudentsByAssignedBy(@Param("counsellorId") Long counsellorId);

    @Query("SELECT s FROM Student s LEFT JOIN s.user u " +
           "WHERE (:status IS NULL OR s.status = :status) " +
           "AND (LOWER(u.firstName) LIKE :search " +
           "OR LOWER(u.lastName) LIKE :search " +
           "OR LOWER(s.email) LIKE :search " +
           "OR u.phone LIKE :search " +
           "OR u IS NULL)")
    Page<Student> findByFilters(@Param("status") StudentStatus status, @Param("search") String search, Pageable pageable);
    
    // Branch-based access control methods
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId)", nativeQuery = true)
    Page<Student> findAllWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND (:status IS NULL OR s.status = :status)", nativeQuery = true)
    Page<Student> findByStatusWithAccess(@Param("status") String status, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT COUNT(s) FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId)", nativeQuery = true)
    long countWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR u.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersWithAccess(@Param("status") String status, @Param("search") String search, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.assignedBy_id = :counsellorId AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR u.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersForCounsellor(@Param("status") String status, @Param("search") String search, @Param("counsellorId") Long counsellorId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.created_by = :creatorId AND (:status IS NULL OR s.status = :status) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR u.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByFiltersForCreator(@Param("status") String status, @Param("search") String search, @Param("creatorId") Long creatorId, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.branch.id = :branchId")
    Long countStudentsByBranchId(@Param("branchId") Long branchId);
    
    // Methods for finding non-registered students (status != REGISTERED) with optional status filter
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id LEFT JOIN countries c ON s.country_id = c.id LEFT JOIN users creator ON s.created_by = creator.id LEFT JOIN roles r ON creator.role_id = r.id WHERE (:isAdmin = true OR s.branch_id IS NULL OR s.branch_id = :branchId) AND s.status != 'REGISTERED' AND (:status IS NULL OR s.status = :status) AND (:countryName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :countryName, '%'))) AND (:dateFrom IS NULL OR s.created_at >= CAST(:dateFrom AS timestamp)) AND (:dateTo IS NULL OR s.created_at <= CAST(:dateTo AS timestamp)) AND (:source IS NULL OR (r.name IN (:sourceRoles) OR (s.source_type IN (:sourceTypes) AND s.source_type IS NOT NULL))) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR u.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusWithAccess(@Param("search") String search, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, @Param("status") String status, @Param("countryName") String countryName, @Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo, @Param("source") String source, @Param("sourceRoles") List<String> sourceRoles, @Param("sourceTypes") List<String> sourceTypes, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id LEFT JOIN countries c ON s.country_id = c.id LEFT JOIN users creator ON s.created_by = creator.id LEFT JOIN roles r ON creator.role_id = r.id WHERE s.assignedBy_id = :counsellorId AND s.status != 'REGISTERED' AND (:status IS NULL OR s.status = :status) AND (:countryName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :countryName, '%'))) AND (:dateFrom IS NULL OR s.created_at >= CAST(:dateFrom AS timestamp)) AND (:dateTo IS NULL OR s.created_at <= CAST(:dateTo AS timestamp)) AND (:source IS NULL OR (r.name IN (:sourceRoles) OR (s.source_type IN (:sourceTypes) AND s.source_type IS NOT NULL))) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR u.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusForCounsellor(@Param("search") String search, @Param("counsellorId") Long counsellorId, @Param("status") String status, @Param("countryName") String countryName, @Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo, @Param("source") String source, @Param("sourceRoles") List<String> sourceRoles, @Param("sourceTypes") List<String> sourceTypes, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s LEFT JOIN users u ON s.user_id = u.id LEFT JOIN countries c ON s.country_id = c.id LEFT JOIN users creator ON s.created_by = creator.id LEFT JOIN roles r ON creator.role_id = r.id WHERE s.created_by = :creatorId AND s.status != 'REGISTERED' AND (:status IS NULL OR s.status = :status) AND (:countryName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :countryName, '%'))) AND (:dateFrom IS NULL OR s.created_at >= CAST(:dateFrom AS timestamp)) AND (:dateTo IS NULL OR s.created_at <= CAST(:dateTo AS timestamp)) AND (:source IS NULL OR (r.name IN (:sourceRoles) OR (s.source_type IN (:sourceTypes) AND s.source_type IS NOT NULL))) AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(s.email) LIKE :search OR s.phone LIKE :search OR u IS NULL)", nativeQuery = true)
    Page<Student> findByNonRegisteredStatusForCreator(@Param("search") String search, @Param("creatorId") Long creatorId, @Param("status") String status, @Param("countryName") String countryName, @Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo, @Param("source") String source, @Param("sourceRoles") List<String> sourceRoles, @Param("sourceTypes") List<String> sourceTypes, Pageable pageable);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, u.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy IN " +
           "(SELECT u.id FROM User u JOIN u.role r WHERE r.name IN ('REFERRAL', 'COMPANY')) " +
           "OR p.sourceType IN ('REFERRAL', 'COMPANY'))")
    Page<StudentWithStudentPaymentDto> findStudentsWithPaymentByReferralAndCompany(Pageable pageable);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, u.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy = :referralId " +
           "OR (p.sourceType = 'REFERRAL' AND p.sourceId = :referralId))")
    Page<StudentWithStudentPaymentDto> findStudentsWithPaymentByReferral(@Param("referralId") Long referralId, Pageable pageable);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, u.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "LEFT JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE p.isDeleted = false " +
           "AND (s.createdBy = :companyId " +
           "OR (p.sourceType = 'COMPANY' AND p.sourceId = :companyId))")
    Page<StudentWithStudentPaymentDto> findStudentsWithPaymentByCompany(@Param("companyId") Long companyId, Pageable pageable);
    
    @Query("SELECT new com.lab.atlasmentor.dto.StudentWithStudentPaymentDto(" +
           "s.id, u.firstName, u.lastName, s.email, u.phone, s.status, " +
           "s.courseName, s.intakePeriod, s.notes, b.name, s.createdAt, " +
           "p.id, COALESCE(p.assignedAmount, 0), COALESCE(p.paidAmount, 0), p.paymentStatus, p.sourceType, p.sourceId, " +
           "p.isAmountLocked, p.branchId, p.notes, p.createdAt, " +
           "CASE WHEN p.assignedAmount IS NOT NULL AND p.assignedAmount > 0 THEN com.lab.atlasmentor.enums.ApprovalStatus.PENDING ELSE com.lab.atlasmentor.enums.ApprovalStatus.NOT_APPLICABLE END) " +
           "FROM Student s " +
           "INNER JOIN s.user u " +
           "INNER JOIN s.branch b " +
           "INNER JOIN com.lab.atlasmentor.model.StudentPayment p ON p.student.id = s.id " +
           "WHERE s.branch.id = :branchId " +
           "AND p.isDeleted = false " +
           "AND (s.createdBy IN " +
           "(SELECT u.id FROM User u JOIN u.role r WHERE r.name IN ('REFERRAL', 'COMPANY')) " +
           "OR p.sourceType IN ('REFERRAL', 'COMPANY'))")
    Page<StudentWithStudentPaymentDto> findStudentsWithPaymentByBranch(@Param("branchId") Long branchId, Pageable pageable);

    @Modifying
    @Query("UPDATE Student s SET s.assignedBy = null WHERE s.assignedBy.id = :userId")
    void nullifyAssignedByByUserId(@Param("userId") Long userId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT status, COUNT(*) FROM students WHERE created_at >= :from GROUP BY status", nativeQuery = true)
    List<Object[]> countGroupByStatus(@Param("from") LocalDateTime from);

    @Query(value = "SELECT enhanced_status, COUNT(*) FROM students WHERE enhanced_status IS NOT NULL AND created_at >= :from GROUP BY enhanced_status", nativeQuery = true)
    List<Object[]> countGroupByEnhancedStatus(@Param("from") LocalDateTime from);

    @Query(value = "SELECT source_type, COUNT(*) FROM students WHERE source_type IS NOT NULL AND created_at >= :from GROUP BY source_type", nativeQuery = true)
    List<Object[]> countGroupBySourceType(@Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM students WHERE created_at >= :from AND created_at < :to", nativeQuery = true)
    Long countNewBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'Mon') as month, COUNT(*) as cnt " +
           "FROM students WHERE created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', created_at) ORDER BY DATE_TRUNC('month', created_at)", nativeQuery = true)
    List<Object[]> countMonthlyNewStudents(@Param("from") LocalDateTime from);

    @Query(value = "SELECT c.name, " +
           "COUNT(CASE WHEN s.created_at >= :from THEN 1 END) as student_count, " +
           "COUNT(CASE WHEN s.status = 'REGISTERED' THEN 1 END) as converted_all_time, " +
           "COUNT(s.id) as total_all_time " +
           "FROM students s " +
           "LEFT JOIN countries c ON s.country_id = c.id " +
           "WHERE c.id IS NOT NULL " +
           "GROUP BY c.id, c.name " +
           "ORDER BY CASE WHEN COUNT(s.id) = 0 THEN 0 ELSE COUNT(CASE WHEN s.status = 'REGISTERED' THEN 1 END) * 100.0 / COUNT(s.id) END DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopCountries(@Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', s.created_at), 'Mon') as month, " +
           "c.name as country, COUNT(s.id) as cnt " +
           "FROM students s " +
           "JOIN countries c ON s.country_id = c.id " +
           "WHERE s.created_at >= :from " +
           "AND c.name IN (:countryNames) " +
           "GROUP BY DATE_TRUNC('month', s.created_at), c.name " +
           "ORDER BY DATE_TRUNC('month', s.created_at), c.name", nativeQuery = true)
    List<Object[]> findMonthlyIntakeByCountry(@Param("from") LocalDateTime from, @Param("countryNames") List<String> countryNames);

    // ==================== BRANCH-SCOPED DASHBOARD QUERIES ====================

    @Query(value = "SELECT status, COUNT(*) FROM students WHERE branch_id = :branchId AND created_at >= :from GROUP BY status", nativeQuery = true)
    List<Object[]> countGroupByStatusForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT enhanced_status, COUNT(*) FROM students WHERE branch_id = :branchId AND enhanced_status IS NOT NULL AND created_at >= :from GROUP BY enhanced_status", nativeQuery = true)
    List<Object[]> countGroupByEnhancedStatusForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT source_type, COUNT(*) FROM students WHERE branch_id = :branchId AND source_type IS NOT NULL AND created_at >= :from GROUP BY source_type", nativeQuery = true)
    List<Object[]> countGroupBySourceTypeForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM students WHERE branch_id = :branchId AND created_at >= :from AND created_at < :to", nativeQuery = true)
    Long countNewBetweenForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM students WHERE branch_id = :branchId", nativeQuery = true)
    Long countByBranchId(@Param("branchId") Long branchId);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'Mon') as month, COUNT(*) as cnt " +
           "FROM students WHERE branch_id = :branchId AND created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', created_at) ORDER BY DATE_TRUNC('month', created_at)", nativeQuery = true)
    List<Object[]> countMonthlyNewStudentsForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT c.name, " +
           "COUNT(CASE WHEN s.created_at >= :from THEN 1 END) as student_count, " +
           "COUNT(CASE WHEN s.status = 'REGISTERED' THEN 1 END) as converted_all_time, " +
           "COUNT(s.id) as total_all_time " +
           "FROM students s " +
           "LEFT JOIN countries c ON s.country_id = c.id " +
           "WHERE c.id IS NOT NULL AND s.branch_id = :branchId " +
           "GROUP BY c.id, c.name " +
           "ORDER BY CASE WHEN COUNT(s.id) = 0 THEN 0 ELSE COUNT(CASE WHEN s.status = 'REGISTERED' THEN 1 END) * 100.0 / COUNT(s.id) END DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopCountriesForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', s.created_at), 'Mon') as month, " +
           "c.name as country, COUNT(s.id) as cnt " +
           "FROM students s " +
           "JOIN countries c ON s.country_id = c.id " +
           "WHERE s.branch_id = :branchId AND s.created_at >= :from " +
           "AND c.name IN (:countryNames) " +
           "GROUP BY DATE_TRUNC('month', s.created_at), c.name " +
           "ORDER BY DATE_TRUNC('month', s.created_at), c.name", nativeQuery = true)
    List<Object[]> findMonthlyIntakeByCountryForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from, @Param("countryNames") List<String> countryNames);
}




