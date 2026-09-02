package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.TaskAttachmentResponse;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskAttachment;
import com.lab.atlasmentor.model.TaskComment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskAttachmentRepository;
import com.lab.atlasmentor.repository.TaskCommentRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The one shared upload path both the proof section (commentId omitted) and comment
 * section (commentId present) call through - see TaskAttachmentUploadService's
 * javadoc. Compression itself is exercised separately in MediaCompressionServiceTest;
 * this focuses on validation ordering (type -> size -> compression) and the
 * comment_id distinction.
 */
@ExtendWith(MockitoExtension.class)
class TaskAttachmentUploadServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskCommentRepository taskCommentRepository;
    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;
    @Mock
    private TaskActivityRepository taskActivityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MediaCompressionService mediaCompressionService;
    @Mock
    private R2StorageService r2StorageService;

    @InjectMocks
    private TaskAttachmentUploadService service;

    private Task task;
    private User uploader;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxVideoSizeMb", 25L);
        ReflectionTestUtils.setField(service, "maxDefaultSizeMb", 10L);

        task = new Task();
        task.setId(1L);

        uploader = new User();
        uploader.setId(200L);
        uploader.setFirstName("Employee");
        uploader.setLastName("One");
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3});
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadAttachment(1L, file, null, 200L));
        assertTrue(ex.getMessage().contains("Unsupported file type"));
        assertTrue(ex.getMessage().contains("pdf"));
        verifyNoInteractions(mediaCompressionService, r2StorageService);
    }

    @Test
    void rejectsOversizedVideoBeforeCompressing() {
        byte[] tooLarge = new byte[(int) (26L * 1024 * 1024)]; // > 25MB video cap
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", tooLarge);
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadAttachment(1L, file, null, 200L));
        assertTrue(ex.getMessage().contains("too large"));
        verifyNoInteractions(mediaCompressionService, r2StorageService);
    }

    @Test
    void rejectsOversizedImageAtTheDefaultCap() {
        byte[] tooLarge = new byte[(int) (11L * 1024 * 1024)]; // > 10MB default cap
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", tooLarge);
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> service.uploadAttachment(1L, file, null, 200L));
        verifyNoInteractions(mediaCompressionService, r2StorageService);
    }

    @Test
    void proofSectionUploadLeavesCommentNull() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(200L)).thenReturn(Optional.of(uploader));
        when(mediaCompressionService.compress(any(), anyString(), any(), anyString()))
                .thenReturn(new MediaCompressionService.CompressedFile(new byte[]{9, 9}, "photo.jpg", "image/jpeg", true));
        when(r2StorageService.upload(anyString(), any(), anyString())).thenReturn("https://cdn.example.com/tasks/1/photo.jpg");
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskAttachmentResponse response = service.uploadAttachment(1L, file, null, 200L);

        assertNull(response.getCommentId());
        assertEquals("https://cdn.example.com/tasks/1/photo.jpg", response.getFileUrl());
        assertEquals(3L, response.getOriginalFileSize());
        assertEquals(2L, response.getFileSize());
        assertEquals("IMAGE", response.getFileType());

        ArgumentCaptor<TaskAttachment> captor = ArgumentCaptor.forClass(TaskAttachment.class);
        verify(taskAttachmentRepository).save(captor.capture());
        assertNull(captor.getValue().getComment());
        verify(taskActivityRepository).save(any());
    }

    @Test
    void commentSectionUploadSetsTheComment() {
        TaskComment comment = new TaskComment();
        comment.setId(55L);
        comment.setTask(task);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(taskCommentRepository.findById(55L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(200L)).thenReturn(Optional.of(uploader));
        when(mediaCompressionService.compress(any(), anyString(), any(), anyString()))
                .thenReturn(new MediaCompressionService.CompressedFile(new byte[]{9, 9}, "photo.jpg", "image/jpeg", true));
        when(r2StorageService.upload(anyString(), any(), anyString())).thenReturn("https://cdn.example.com/tasks/1/photo.jpg");
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskAttachmentResponse response = service.uploadAttachment(1L, file, 55L, 200L);

        assertEquals(55L, response.getCommentId());
    }

    @Test
    void rejectsWhenCommentBelongsToADifferentTask() {
        Task otherTask = new Task();
        otherTask.setId(999L);
        TaskComment comment = new TaskComment();
        comment.setId(55L);
        comment.setTask(otherTask);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(200L)).thenReturn(Optional.of(uploader));
        when(taskCommentRepository.findById(55L)).thenReturn(Optional.of(comment));

        assertThrows(BusinessException.class, () -> service.uploadAttachment(1L, file, 55L, 200L));
        verifyNoInteractions(mediaCompressionService, r2StorageService);
    }
}
