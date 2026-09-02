package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.TaskAttachmentResponse;
import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskActivity;
import com.lab.atlasmentor.model.TaskAttachment;
import com.lab.atlasmentor.model.TaskComment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskAttachmentRepository;
import com.lab.atlasmentor.repository.TaskCommentRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.util.AttachmentTypeUtil;
import com.lab.atlasmentor.util.AttachmentTypeUtil.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The single shared entry point for uploading a task attachment, used by both the
 * proof section (commentId omitted) and the comment section (commentId present) -
 * see the {@code POST /api/tasks/{id}/attachments/upload} endpoint in TaskController.
 * There is deliberately only one upload/compression/R2 code path here; the two
 * contexts differ only in whether {@link TaskAttachment#getComment()} ends up set.
 *
 * Distinct from {@link TaskService#addAttachment}, which only registers a URL a
 * client already uploaded elsewhere (e.g. the lead-import flow) - this method
 * receives the raw file from the employee's device and performs the real upload.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAttachmentUploadService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final UserRepository userRepository;
    private final MediaCompressionService mediaCompressionService;
    private final R2StorageService r2StorageService;

    @Value("${app.attachments.max-size-video-mb}")
    private long maxVideoSizeMb;

    @Value("${app.attachments.max-size-default-mb}")
    private long maxDefaultSizeMb;

    @Transactional
    public TaskAttachmentResponse uploadAttachment(Long taskId, MultipartFile file, Long commentId, Long uploadedByUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("A file is required.");
        }

        Task task = taskRepository.findActiveTaskById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        Category category = AttachmentTypeUtil.categorize(originalFileName);
        if (category == null) {
            throw new BusinessException("Unsupported file type. Accepted types: " + AttachmentTypeUtil.ACCEPTED_EXTENSIONS_MESSAGE);
        }

        // Size cap enforced BEFORE compression - reject oversized files cheaply rather
        // than spending CPU/FFmpeg time on something about to be rejected anyway.
        long maxBytes = (category == Category.VIDEO ? maxVideoSizeMb : maxDefaultSizeMb) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            long maxMb = category == Category.VIDEO ? maxVideoSizeMb : maxDefaultSizeMb;
            throw new BusinessException("File is too large. Maximum size for " + category.name().toLowerCase()
                    + " files is " + maxMb + "MB.");
        }

        User uploadedBy = userRepository.findById(uploadedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + uploadedByUserId));

        TaskComment comment = null;
        if (commentId != null) {
            comment = taskCommentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));
            if (!comment.getTask().getId().equals(taskId)) {
                throw new BusinessException("Comment does not belong to this task");
            }
        }

        byte[] originalContent;
        try {
            originalContent = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file: " + e.getMessage());
        }
        long originalSize = originalContent.length;

        String logLabel = "task " + taskId + " attachment '" + originalFileName + "'";
        MediaCompressionService.CompressedFile compressed =
                mediaCompressionService.compress(originalContent, originalFileName, category, logLabel);

        log.info("Compressed {}: {} bytes -> {} bytes (compressionApplied={})",
                logLabel, originalSize, compressed.content().length, compressed.compressed());

        String key = "tasks/" + taskId + "/" + UUID.randomUUID() + "-" + compressed.fileName();
        String fileUrl = r2StorageService.upload(key, compressed.content(), compressed.contentType());

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setFileName(originalFileName);
        attachment.setFileUrl(fileUrl);
        attachment.setFileSize((long) compressed.content().length);
        attachment.setOriginalFileSize(originalSize);
        attachment.setUploadedBy(uploadedBy);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setComment(comment);
        attachment.setCreatedBy(uploadedBy.getId());
        attachment.setUpdatedBy(uploadedBy.getId());

        TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);

        // Same activity-log pattern as TaskService#addAttachment, for both contexts.
        TaskActivity activity = new TaskActivity(task, TaskAction.ATTACHMENT_ADDED, null, originalFileName, uploadedBy);
        activity.setCreatedBy(uploadedBy.getId());
        taskActivityRepository.save(activity);

        return toResponse(savedAttachment, category);
    }

    private TaskAttachmentResponse toResponse(TaskAttachment attachment, Category category) {
        TaskAttachmentResponse response = new TaskAttachmentResponse();
        response.setId(attachment.getId());
        response.setTaskId(attachment.getTask().getId());
        response.setFileName(attachment.getFileName());
        response.setFileUrl(attachment.getFileUrl());
        response.setFileSize(attachment.getFileSize());
        response.setFileSizeFormatted(formatFileSize(attachment.getFileSize()));
        response.setOriginalFileSize(attachment.getOriginalFileSize());
        response.setFileType(category.name());
        response.setCommentId(attachment.getComment() != null ? attachment.getComment().getId() : null);
        response.setUploadedById(attachment.getUploadedBy().getId());
        response.setUploadedByName(attachment.getUploadedBy().getFullName());
        response.setUploadedAt(attachment.getUploadedAt());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        // Strip any directory components a browser/client might include, keep it simple.
        String base = fileName.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        return slash >= 0 ? base.substring(slash + 1) : base;
    }
}
