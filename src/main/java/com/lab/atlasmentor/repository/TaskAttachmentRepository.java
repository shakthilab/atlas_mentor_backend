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

    /**
     * Whether this task has at least one proof-section attachment - i.e. one attached
     * directly to the task rather than to a specific comment (comment_id IS NULL).
     * Backs the proof-required check in TaskService#updateTaskStatus (V35): a photo
     * attached to a casual comment must not satisfy the requirement.
     */
    boolean existsByTaskIdAndCommentIsNull(Long taskId);

    /**
     * Whether any attachment was ever recorded against this comment - backs
     * TaskService#deleteEmptyComment's guard against deleting a comment that actually
     * has media on it (only a truly-orphaned empty comment, left behind when a
     * media-only comment's follow-up upload fails, may be removed this way).
     */
    boolean existsByCommentId(Long commentId);

    @Query("SELECT SUM(ta.fileSize) FROM TaskAttachment ta WHERE ta.task.id = :taskId")
    Long sumFileSizeByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskAttachment ta WHERE ta.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);
}
