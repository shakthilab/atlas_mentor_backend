package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.model.DayWorkspace;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.model.TemplateDay;
import com.lab.atlasmentor.model.TemplateTask;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.DayWorkspaceRepository;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.TemplateDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateInstantiationServiceTest {

    @Mock
    private TemplateDayRepository templateDayRepository;
    @Mock
    private DayWorkspaceRepository dayWorkspaceRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskActivityRepository taskActivityRepository;
    @Mock
    private TaskDisplayIdService taskDisplayIdService;

    @InjectMocks
    private TemplateInstantiationService service;

    private TaskBundle template;
    private User employee;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        template = new TaskBundle();
        template.setId(10L);
        template.setName("New Hire Onboarding");

        employee = new User();
        employee.setId(100L);

        // Day-of-month 7 - the day number this service must derive for `today`, regardless
        // of how many DayWorkspace rows (if any) the employee already has.
        today = LocalDate.of(2026, 8, 7);
    }

    private TemplateDay dayWithOneTask(int dayNumber) {
        TemplateDay day = new TemplateDay();
        day.setId((long) dayNumber);
        day.setDayNumber(dayNumber);
        day.setIsWeeklyCheckpoint(false);

        TemplateTask task = new TemplateTask();
        task.setId(1000L + dayNumber);
        task.setTitle("Day " + dayNumber + " task");
        task.setPriority(Priority.MEDIUM);
        // Snapshot-copy check (V35): every dayWithOneTask() task requires proof, so
        // createsTodaysCalendarDayRegardlessOfEmployeesPriorWorkspaceCount can assert
        // it lands on the instantiated Task too.
        task.setProofRequired(true);

        List<TemplateTask> tasks = new ArrayList<>();
        tasks.add(task);
        day.setTemplateTasks(tasks);
        return day;
    }

    private TemplateDay dayWithNoTasks(int dayNumber) {
        TemplateDay day = new TemplateDay();
        day.setId((long) dayNumber);
        day.setDayNumber(dayNumber);
        day.setIsWeeklyCheckpoint(false);
        day.setTemplateTasks(new ArrayList<>());
        return day;
    }

    @Test
    void createsTodaysCalendarDayRegardlessOfEmployeesPriorWorkspaceCount() {
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());
        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithOneTask(6), dayWithOneTask(7)));
        when(dayWorkspaceRepository.save(any(DayWorkspace.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "cron");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.CREATED, result.getOutcome());
        assertEquals(7, result.getDayNumber());
        assertEquals(1, result.getTasksCreated());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        assertEquals("Day 7 task", taskCaptor.getValue().getTitle());
        assertEquals(employee, taskCaptor.getValue().getAssignedTo());
        assertEquals(today, taskCaptor.getValue().getExecutionDate());
        // Backs uk_tasks_day_workspace_source_template_task - every template-generated
        // task must record which TemplateTask it came from.
        assertEquals(1007L, taskCaptor.getValue().getSourceTemplateTask().getId());
        // proofRequired (V35) is copied as a one-time snapshot, same as title/priority above.
        assertTrue(taskCaptor.getValue().getProofRequired());

        ArgumentCaptor<DayWorkspace> workspaceCaptor = ArgumentCaptor.forClass(DayWorkspace.class);
        verify(dayWorkspaceRepository).save(workspaceCaptor.capture());
        assertEquals(7, workspaceCaptor.getValue().getDayNumberInMonth());
        // Never consulted: the day number comes from the calendar, not from counting rows.
        verify(dayWorkspaceRepository, never()).countByEmployeeId(any());
    }

    @Test
    void isIdempotentWhenTodaysWorkspaceAlreadyExists() {
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.of(new DayWorkspace()));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "cron");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.ALREADY_INSTANTIATED, result.getOutcome());
        verify(dayWorkspaceRepository, never()).save(any());
        verify(taskRepository, never()).save(any());
        // Must never even look at template days once idempotency short-circuits.
        verifyNoInteractions(templateDayRepository);
    }

    @Test
    void createsEmptyWorkspaceForTodayWhenDayHasNoTasksYet() {
        // Day exists in the template but nobody has attached tasks to it yet (e.g. days
        // 1-5 of the real Senior/Junior Counsellor templates, where content starts at
        // day 6). This must NOT be treated as template exhaustion.
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());
        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithNoTasks(7), dayWithOneTask(8)));
        when(dayWorkspaceRepository.save(any(DayWorkspace.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "cron");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.CREATED, result.getOutcome());
        assertEquals(7, result.getDayNumber());
        assertEquals(0, result.getTasksCreated());

        ArgumentCaptor<DayWorkspace> workspaceCaptor = ArgumentCaptor.forClass(DayWorkspace.class);
        verify(dayWorkspaceRepository, times(1)).save(workspaceCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(workspaceCaptor.getValue().getDailyCompletionPct()));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void skipsWhenTemplateHasNoDayDefinedForTodaysCalendarDate() {
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());
        // Template only has days 1-2 defined; today (day-of-month 7) has no TemplateDay at all.
        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithOneTask(1), dayWithOneTask(2)));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "cron");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.TEMPLATE_EXHAUSTED, result.getOutcome());
        assertEquals(7, result.getDayNumber());
        verify(dayWorkspaceRepository, never()).save(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void neverBackfillsCreatesExactlyOneDaysWorthOfTasksPerCall() {
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());

        TemplateDay day7 = dayWithOneTask(7);
        TemplateTask secondTask = new TemplateTask();
        secondTask.setId(2000L);
        secondTask.setTitle("Second day-7 task");
        secondTask.setPriority(Priority.HIGH);
        day7.getTemplateTasks().add(secondTask);

        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithOneTask(6), day7, dayWithOneTask(8)));
        when(dayWorkspaceRepository.save(any(DayWorkspace.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "cron");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.CREATED, result.getOutcome());
        assertEquals(7, result.getDayNumber());
        assertEquals(2, result.getTasksCreated());
        // Only day 7's two tasks are created - days 6 and 8 are never touched in this call.
        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void treatsWorkspaceUniqueConstraintViolationAsAlreadyInstantiated() {
        // Simulates two triggers (e.g. cron and startup catch-up) racing past the pre-check
        // SELECT at nearly the same moment: this run's SELECT saw no workspace, but by the
        // time it inserts, the other run already committed one - uk_day_workspaces_employee_date
        // fires. Must be treated as a normal skip, not bubble up as an error.
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());
        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithOneTask(7)));
        when(dayWorkspaceRepository.save(any(DayWorkspace.class)))
                .thenThrow(new DataIntegrityViolationException("uk_day_workspaces_employee_date"));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "startup-catch-up");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.ALREADY_INSTANTIATED, result.getOutcome());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void treatsTaskUniqueConstraintViolationAsAlreadyInstantiated() {
        // Same race, but caught one step later: the workspace insert succeeded (this run
        // "won" that part), but a concurrent run had already inserted the task for this
        // exact template task on this workspace - uk_tasks_day_workspace_source_template_task
        // fires. The whole REQUIRES_NEW transaction (workspace included) rolls back, so this
        // must also resolve to a plain skip.
        when(dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today))
                .thenReturn(Optional.empty());
        when(templateDayRepository.findByRoleTemplateId(template.getId()))
                .thenReturn(List.of(dayWithOneTask(7)));
        when(dayWorkspaceRepository.save(any(DayWorkspace.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class)))
                .thenThrow(new DataIntegrityViolationException("uk_tasks_day_workspace_source_template_task"));

        TemplateInstantiationService.InstantiationResult result =
                service.instantiateDayFor(template, employee, today, "periodic-safety-net");

        assertEquals(TemplateInstantiationService.InstantiationResult.Outcome.ALREADY_INSTANTIATED, result.getOutcome());
    }

}
