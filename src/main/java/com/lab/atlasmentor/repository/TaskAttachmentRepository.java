package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId);

    List<TaskAttachment> findByUploadedById(Long uploadedById);

    @Query("SELECT ta FROM TaskAttachment ta JOIN FETCH ta.uploadedBy WHERE ta.task.id = :taskId ORDER BY ta.uploadedAt DESC")
    List<TaskAttachment> findByTaskIdWithUploader(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(ta) FROM TaskAttachment ta WHERE ta.task.id = :taskId")
    Long countByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT SUM(ta.fileSize) FROM TaskAttachment ta WHERE ta.task.id = :taskId")
    Long sumFileSizeByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskAttachment ta WHERE ta.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);
}
