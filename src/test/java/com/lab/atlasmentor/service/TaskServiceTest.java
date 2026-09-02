package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.AddCommentRequest;
import com.lab.atlasmentor.dto.TaskCommentResponse;
import com.lab.atlasmentor.dto.UpdateCommentRequest;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskComment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskAttachmentRepository;
import com.lab.atlasmentor.repository.TaskCommentRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.security.AccessScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Focused on the proof-required enforcement added to updateTaskStatus (V35) - see
 * TaskAttachmentRepository#existsByTaskIdAndCommentIsNull. This deliberately checks
 * for a proof-context attachment (comment_id IS NULL) rather than any attachment on
 * the task, so a photo attached to a casual comment can never satisfy the requirement.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;
    @Mock
    private TaskActivityRepository taskActivityRepository;
    @Mock
    private TaskCommentRepository taskCommentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccessScopeService accessScopeService;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private User admin;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");

        admin = new User();
        admin.setId(100L);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(adminRole);

        task = new Task("Submit report", "desc", admin, admin, admin, Priority.MEDIUM, LocalDate.now(), null);
        task.setId(1L);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setProofRequired(true);

        // lenient: the deleteEmptyComment tests below never touch task/user lookups
        lenient().when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        lenient().when(userRepository.findById(100L)).thenReturn(Optional.of(admin));
    }

    @Test
    void markingDoneWithoutProofAttachmentIsRejected() {
        when(taskAttachmentRepository.existsByTaskIdAndCommentIsNull(1L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskService.updateTaskStatus(1L, TaskStatus.DONE, 100L));
        assertTrue(ex.getMessage().toLowerCase().contains("proof"));
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus(), "status must not change when proof is missing");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void markingDoneWithAProofAttachmentSucceeds() {
        when(taskAttachmentRepository.existsByTaskIdAndCommentIsNull(1L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.updateTaskStatus(1L, TaskStatus.DONE, 100L);

        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @Test
    void commentOnlyAttachmentDoesNotSatisfyProofRequirement() {
        // A photo attached to a casual comment (comment_id IS NOT NULL) must not count -
        // existsByTaskIdAndCommentIsNull specifically excludes it, so this mirrors the
        // repository-level guarantee at the service layer.
        when(taskAttachmentRepository.existsByTaskIdAndCommentIsNull(1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> taskService.updateTaskStatus(1L, TaskStatus.DONE, 100L));
    }

    @Test
    void proofNotRequiredSkipsTheCheckEntirely() {
        task.setProofRequired(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.updateTaskStatus(1L, TaskStatus.DONE, 100L);

        assertEquals(TaskStatus.DONE, task.getStatus());
        verifyNoInteractions(taskAttachmentRepository);
    }

    // ---- Comment validation: empty/blank comments are strictly rejected ----

    @Test
    void nullCommentTextIsRejected() {
        AddCommentRequest request = new AddCommentRequest();
        request.setComment(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskService.addComment(1L, request, 100L));
        assertEquals("Comment cannot be empty", ex.getMessage());
        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    @Test
    void emptyCommentTextIsRejected() {
        AddCommentRequest request = new AddCommentRequest();
        request.setComment("");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskService.addComment(1L, request, 100L));
        assertEquals("Comment cannot be empty", ex.getMessage());
        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    @Test
    void whitespaceOnlyCommentTextIsRejected() {
        AddCommentRequest request = new AddCommentRequest();
        request.setComment("   ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskService.addComment(1L, request, 100L));
        assertEquals("Comment cannot be empty", ex.getMessage());
        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    @Test
    void normalCommentTextIsStillStoredAsGiven() {
        AddCommentRequest request = new AddCommentRequest();
        request.setComment("Looks good to me");

        when(taskCommentRepository.save(any(TaskComment.class))).thenAnswer(inv -> {
            TaskComment saved = inv.getArgument(0);
            saved.setId(502L);
            return saved;
        });

        TaskCommentResponse response = taskService.addComment(1L, request, 100L);

        assertEquals("Looks good to me", response.getComment());
    }

    @Test
    void updatingCommentWithEmptyTextIsRejected() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setComment("   ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskService.updateComment(1L, 500L, request, 100L));
        assertEquals("Comment cannot be empty", ex.getMessage());
        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    // ---- Delete comment ----

    @Test
    void deletingCommentByItsOwnAuthorSucceeds() {
        TaskComment comment = new TaskComment(task, "Test comment", admin);
        comment.setId(600L);
        when(taskCommentRepository.findById(600L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(100L)).thenReturn(Optional.of(admin));

        taskService.deleteComment(1L, 600L, 100L);

        verify(taskCommentRepository).delete(comment);
    }

    @Test
    void deletingCommentByAdminSucceedsEvenIfNotAuthor() {
        Role employeeRole = new Role();
        employeeRole.setId(2L);
        employeeRole.setName("EMPLOYEE");
        User author = new User();
        author.setId(200L);
        author.setRole(employeeRole);

        TaskComment comment = new TaskComment(task, "Employee comment", author);
        comment.setId(601L);
        when(taskCommentRepository.findById(601L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(100L)).thenReturn(Optional.of(admin)); // admin role

        taskService.deleteComment(1L, 601L, 100L);

        verify(taskCommentRepository).delete(comment);
    }

    @Test
    void deletingSomeoneElsesCommentByNonAdminIsRejected() {
        Role employeeRole = new Role();
        employeeRole.setId(2L);
        employeeRole.setName("EMPLOYEE");
        User someoneElse = new User();
        someoneElse.setId(200L);
        someoneElse.setRole(employeeRole);

        User caller = new User();
        caller.setId(300L);
        caller.setRole(employeeRole);

        TaskComment comment = new TaskComment(task, "Hello", someoneElse);
        comment.setId(602L);
        when(taskCommentRepository.findById(602L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(300L)).thenReturn(Optional.of(caller));

        BusinessException ex = assertThrows(BusinessException.class, () -> taskService.deleteComment(1L, 602L, 300L));
        assertEquals("You can only delete your own comments", ex.getMessage());
        verify(taskCommentRepository, never()).delete(any(TaskComment.class));
    }

    @Test
    void deletingCommentBelongingToDifferentTaskIsRejected() {
        Task otherTask = new Task();
        otherTask.setId(999L);
        TaskComment comment = new TaskComment(otherTask, "Hello", admin);
        comment.setId(603L);
        when(taskCommentRepository.findById(603L)).thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(BusinessException.class, () -> taskService.deleteComment(1L, 603L, 100L));
        assertEquals("Comment does not belong to this task", ex.getMessage());
        verify(taskCommentRepository, never()).delete(any(TaskComment.class));
    }

    @Test
    void deleteTaskByAdminSucceeds() {
        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(100L)).thenReturn(Optional.of(admin));

        taskService.deleteTask(1L, 100L);

        assertTrue(task.getIsDeleted());
        assertEquals(100L, task.getUpdatedBy());
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTaskByBranchPartnerSucceedsForSameBranch() {
        com.lab.atlasmentor.model.Branch branch = new com.lab.atlasmentor.model.Branch();
        branch.setId(10L);
        task.setBranch(branch);

        Role bpRole = new Role();
        bpRole.setId(5L);
        bpRole.setName("BRANCH_PARTNER");

        User bpUser = new User();
        bpUser.setId(500L);
        bpUser.setRole(bpRole);
        bpUser.setBranch(branch);

        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(500L)).thenReturn(Optional.of(bpUser));

        taskService.deleteTask(1L, 500L);

        assertTrue(task.getIsDeleted());
        assertEquals(500L, task.getUpdatedBy());
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTaskByManagerSucceedsForSameBranch() {
        com.lab.atlasmentor.model.Branch branch = new com.lab.atlasmentor.model.Branch();
        branch.setId(10L);
        task.setBranch(branch);

        Role managerRole = new Role();
        managerRole.setId(4L);
        managerRole.setName("MANAGER");

        User managerUser = new User();
        managerUser.setId(400L);
        managerUser.setRole(managerRole);
        managerUser.setBranch(branch);

        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(400L)).thenReturn(Optional.of(managerUser));

        taskService.deleteTask(1L, 400L);

        assertTrue(task.getIsDeleted());
        assertEquals(400L, task.getUpdatedBy());
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTasksBulkByAdminSucceeds() {
        Task task2 = new Task();
        task2.setId(2L);
        task2.setIsDeleted(false);

        when(taskRepository.findActiveTaskById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findActiveTaskById(2L)).thenReturn(Optional.of(task2));
        when(userRepository.findById(100L)).thenReturn(Optional.of(admin));

        com.lab.atlasmentor.dto.BulkDeleteTaskResponse response =
                taskService.deleteTasks(java.util.List.of(1L, 2L), 100L);

        assertEquals(2, response.getDeletedCount());
        assertEquals(java.util.List.of(1L, 2L), response.getDeletedTaskIds());
        assertTrue(response.getFailedTaskIds().isEmpty());
        assertTrue(task.getIsDeleted());
        assertTrue(task2.getIsDeleted());
        verify(taskRepository, times(2)).save(any(Task.class));
    }
}
