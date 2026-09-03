package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.DayDetailResponse;
import com.lab.atlasmentor.model.DayWorkspace;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskComment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.DayWorkspaceRepository;
import com.lab.atlasmentor.repository.TaskCommentRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.security.AccessScopeService;
import com.lab.atlasmentor.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTreeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DayWorkspaceRepository dayWorkspaceRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskCommentRepository taskCommentRepository;
    @Mock
    private AccessScopeService accessScopeService;

    @InjectMocks
    private EmployeeTreeService employeeTreeService;

    private DayWorkspace workspace;
    private Task task;
    private LocalDate workDate;

    @BeforeEach
    void setUp() {
        workDate = LocalDate.of(2026, 8, 20);

        User employee = new User();
        employee.setId(34L);
        employee.setFirstName("Test");
        employee.setLastName("Employee");

        workspace = new DayWorkspace();
        workspace.setId(17L);
        workspace.setEmployee(employee);
        workspace.setWorkDate(workDate);
        workspace.setApprovalStage("NOT_STARTED");

        task = new Task();
        task.setId(140L);
        task.setDayWorkspace(workspace);

        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(34L, workDate)).thenReturn(Optional.of(workspace));
        when(taskRepository.findByDayWorkspaceId(17L)).thenReturn(List.of(task));
        when(taskRepository.findByDayWorkspaceEmployeeIdAndWorkDateBetween(anyLong(), any(), any())).thenReturn(List.of());
    }

    // A voice-only comment (no typed text, see AddCommentRequest) is now a valid
    // task_comments row - the day-detail summary must not NPE building its preview.
    @Test
    void voiceOnlyLatestCommentFallsBackToPlaceholderInsteadOfNpeing() {
        TaskComment voiceComment = new TaskComment(task, null, new User());
        voiceComment.setId(900L);
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(140L)).thenReturn(List.of(voiceComment));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("ADMIN");

            DayDetailResponse response = employeeTreeService.getDayDetail(34L, workDate);

            assertEquals("🎤 Voice message", response.getTasks().get(0).getLatestCommentPreview());
        }
    }

    @Test
    void textCommentIsPreviewedAsGiven() {
        TaskComment textComment = new TaskComment(task, "Looks good", new User());
        textComment.setId(901L);
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(140L)).thenReturn(List.of(textComment));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("ADMIN");

            DayDetailResponse response = employeeTreeService.getDayDetail(34L, workDate);

            assertEquals("Looks good", response.getTasks().get(0).getLatestCommentPreview());
        }
    }
}
