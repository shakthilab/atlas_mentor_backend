package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.StudentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {
    
    List<StudentNote> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
