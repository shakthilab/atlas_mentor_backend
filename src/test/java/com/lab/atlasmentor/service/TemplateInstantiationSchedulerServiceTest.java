package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskBundleRepository;
import com.lab.atlasmentor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies the three trigger entry points (cron, startup catch-up, periodic safety net) all
 * funnel through the exact same {@link TemplateInstantiationSchedulerService#instantiateForDate}
 * -> {@link TemplateInstantiationService#instantiateDayFor} path, tagged with the right trigger
 * source for each, and that a day already instantiated makes any of them a no-op.
 */
@ExtendWith(MockitoExtension.class)
class TemplateInstantiationSchedulerServiceTest {

    @Mock
    private TaskBundleRepository taskBundleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TemplateInstantiationService templateInstantiationService;

    @InjectMocks
    private TemplateInstantiationSchedulerService scheduler;

    private TaskBundle activeTemplate;
    private User employee;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(7L);

        activeTemplate = new TaskBundle();
        activeTemplate.setId(11L);
        activeTemplate.setName("Senior Counsellor Aug Template");
        activeTemplate.setStatus(BundleStatus.ACTIVE);
        activeTemplate.setIsDeleted(false);
        activeTemplate.setRole(role);

        employee = new User();
        employee.setId(9L);

        when(taskBundleRepository.findAll()).thenReturn(List.of(activeTemplate));
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));
    }

    @Test
    void nightlyCronTagsCallsWithCronSource() {
        when(templateInstantiationService.instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("cron")))
                .thenReturn(TemplateInstantiationService.InstantiationResult.created(employee.getId(), 1, 3));

        scheduler.runNightlyInstantiation();

        verify(templateInstantiationService, times(1))
                .instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("cron"));
    }

    @Test
    void startupCatchUpTagsCallsWithCatchUpSourceAndReusesTheSameServiceMethod() {
        when(templateInstantiationService.instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("startup-catch-up")))
                .thenReturn(TemplateInstantiationService.InstantiationResult.created(employee.getId(), 1, 3));

        scheduler.runStartupCatchUp();

        verify(templateInstantiationService, times(1))
                .instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("startup-catch-up"));
    }

    @Test
    void periodicSafetyNetTagsCallsWithSafetyNetSource() {
        when(templateInstantiationService.instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("periodic-safety-net")))
                .thenReturn(TemplateInstantiationService.InstantiationResult.created(employee.getId(), 1, 3));

        scheduler.runPeriodicSafetyNet();

        verify(templateInstantiationService, times(1))
                .instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("periodic-safety-net"));
    }

    @Test
    void isNoOpWhenTodayWasAlreadyInstantiatedRegardlessOfWhichTriggerAsks() {
        // The underlying idempotency check (in the real service) already returned
        // ALREADY_INSTANTIATED - simulated here since this service is a pure orchestrator and
        // never re-implements that check itself.
        when(templateInstantiationService.instantiateDayFor(any(), any(), any(), anyString()))
                .thenReturn(TemplateInstantiationService.InstantiationResult.alreadyInstantiated(employee.getId()));

        TemplateInstantiationSchedulerService.RunSummary cronSummary =
                scheduler.instantiateForDate(LocalDate.now(), "cron");
        TemplateInstantiationSchedulerService.RunSummary catchUpSummary =
                scheduler.instantiateForDate(LocalDate.now(), "startup-catch-up");

        assertEquals(0, cronSummary.getTasksCreated());
        assertEquals(1, cronSummary.getEmployeesSkippedAlreadyInstantiated());
        assertEquals(0, catchUpSummary.getTasksCreated());
        assertEquals(1, catchUpSummary.getEmployeesSkippedAlreadyInstantiated());
    }

    @Test
    void twoTriggersRacingForTheSameEmployeeDayResultInExactlyOneCreatedOutcome() {
        // Models the DB-level race guard living in the real service: first caller creates,
        // second caller (racing moments later) hits the unique-constraint catch and comes
        // back ALREADY_INSTANTIATED. From the scheduler's point of view that's just whatever
        // the service returns - it doesn't itself dedupe, which is the point: the service's
        // DataIntegrityViolationException handling is the only thing making this safe.
        when(templateInstantiationService.instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("cron")))
                .thenReturn(TemplateInstantiationService.InstantiationResult.created(employee.getId(), 1, 3));
        when(templateInstantiationService.instantiateDayFor(eq(activeTemplate), eq(employee), any(LocalDate.class), eq("startup-catch-up")))
                .thenReturn(TemplateInstantiationService.InstantiationResult.alreadyInstantiated(employee.getId()));

        LocalDate today = LocalDate.now();
        TemplateInstantiationSchedulerService.RunSummary cronSummary = scheduler.instantiateForDate(today, "cron");
        TemplateInstantiationSchedulerService.RunSummary catchUpSummary = scheduler.instantiateForDate(today, "startup-catch-up");

        assertEquals(3, cronSummary.getTasksCreated());
        assertEquals(0, cronSummary.getEmployeesSkippedAlreadyInstantiated());
        assertEquals(0, catchUpSummary.getTasksCreated());
        assertEquals(1, catchUpSummary.getEmployeesSkippedAlreadyInstantiated());
    }
}
