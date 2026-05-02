package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.assignedBy.id = :counsellorId")
    long countStudentsByAssignedBy(@Param("counsellorId") Long counsellorId);

    @Query("SELECT s FROM Student s JOIN s.user u " +
           "WHERE (:status IS NULL OR s.status = :status) " +
           "AND (LOWER(u.firstName) LIKE :search " +
           "OR LOWER(u.lastName) LIKE :search " +
           "OR LOWER(s.email) LIKE :search " +
           "OR s.phone LIKE :search)")
    Page<Student> findByFilters(@Param("status") StudentStatus status, @Param("search") String search, Pageable pageable);
    
    // Branch-based access control methods
    @Query(value = "SELECT s.* FROM students s JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR u.branch_id = :branchId) ORDER BY s.created_at DESC", nativeQuery = true)
    Page<Student> findAllWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT s.* FROM students s JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR u.branch_id = :branchId) AND (:status IS NULL OR s.status = :status) ORDER BY s.created_at DESC", nativeQuery = true)
    Page<Student> findByStatusWithAccess(@Param("status") String status, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId, Pageable pageable);
    
    @Query(value = "SELECT COUNT(s) FROM students s JOIN users u ON s.user_id = u.id WHERE (:isAdmin = true OR u.branch_id = :branchId)", nativeQuery = true)
    long countWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
}




