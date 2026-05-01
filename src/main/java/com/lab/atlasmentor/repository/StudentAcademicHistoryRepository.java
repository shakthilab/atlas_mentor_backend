package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.StudentAcademicHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAcademicHistoryRepository extends JpaRepository<StudentAcademicHistory, Long> {
    List<StudentAcademicHistory> findByStudentId(Long studentId);
}
