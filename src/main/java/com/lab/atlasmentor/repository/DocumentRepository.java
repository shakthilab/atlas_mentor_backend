package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);

    @Modifying
    @Query("UPDATE Document d SET d.uploadedBy = null WHERE d.uploadedBy.id = :userId")
    void nullifyUploadedByUserId(@Param("userId") Long userId);
}
