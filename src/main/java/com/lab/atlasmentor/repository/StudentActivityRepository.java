package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.StudentActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudentActivityRepository extends JpaRepository<StudentActivity, Long> {
    
    List<StudentActivity> findByStudentIdOrderByPerformedAtDesc(Long studentId);
    
    @Query("SELECT sa FROM StudentActivity sa WHERE sa.student = :student ORDER BY sa.performedAt DESC")
    List<StudentActivity> findByStudentOrderByPerformedAtDesc(@Param("student") Student student);
    
    @Query("SELECT sa FROM StudentActivity sa WHERE sa.student.id = :studentId AND sa.action = :action ORDER BY sa.performedAt DESC")
    List<StudentActivity> findByStudentIdAndActionOrderByPerformedAtDesc(@Param("studentId") Long studentId, @Param("action") StudentStatus action);
    
    @Query("SELECT sa FROM StudentActivity sa WHERE sa.performedAt >= :since ORDER BY sa.performedAt DESC")
    List<StudentActivity> findByPerformedAtAfterOrderByPerformedAtDesc(@Param("since") LocalDateTime since);
    
    @Query("SELECT sa FROM StudentActivity sa WHERE sa.performedBy.id = :userId ORDER BY sa.performedAt DESC")
    List<StudentActivity> findByPerformedByOrderByPerformedAtDesc(@Param("userId") Long userId);
}
