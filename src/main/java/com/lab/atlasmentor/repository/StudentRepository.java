package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Student;
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
}
