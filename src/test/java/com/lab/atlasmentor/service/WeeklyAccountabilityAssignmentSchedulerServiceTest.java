package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityAssignment;
import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityAssignmentRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies the three trigger entry points (cron, startup catch-up, periodic safety net) all
 * funnel through {@link WeeklyAccountabilityAssignmentSchedulerService#runAssignment}, that the
 * persisted {@link WeeklyAccountabilityAssignment} - not a live date-range recompute - is what
 * gets written, and that it advances one week per Saturday regardless of whether the cycle
 * month happens to start on a Saturday.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyAccountabilityAssignmentSchedulerServiceTest {

    @Mock
    private WeeklyAccountabilityTemplateRepository templateRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyAccountabilityAssignmentRepository assignmentRepository;

    private WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;
    private WeeklyAccountabilityAssignmentSchedulerService scheduler;

    private WeeklyAccountabilityTemplate template;
    private User employee;

    /**
     * The real {@link WeeklyAccountabilityTemplateService}, not a mock - the Saturday-week
     * arithmetic ({@code computeSaturdayWeekNumber}) and week-by-number lookup it provides are
     * plain, deterministic logic worth exercising for real rather than stubbing away.
     */
    @BeforeEach
    void setUp() {
        weeklyAccountabilityTemplateService = new WeeklyAccountabilityTemplateService(
                templateRepository, null, null, assignmentRepository);
        scheduler = new WeeklyAccountabilityAssignmentSchedulerService(
                templateRepository, userRepository, weeklyAccountabilityTemplateService, assignmentRepository);

        Role role = new Role();
        role.setId(7L);

        template = new WeeklyAccountabilityTemplate();
        template.setId(11L);
        template.setName("Junior Counsellor Weekly Template");
        template.setStatus(BundleStatus.ACTIVE);
        template.setTargetRole(role);
        template.setCycleMonth(LocalDate.of(2026, 8, 1)); // starts on a Saturday

        List<WeeklyAccountabilityWeek> weeks = new ArrayList<>();
        for (int weekNumber = 1; weekNumber <= 4; weekNumber++) {
            WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
            week.setWeekNumber(weekNumber);
            WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
            question.setId((long) weekNumber);
            question.setQuestionText("Question for week " + weekNumber);
            week.setQuestions(new ArrayList<>(List.of(question)));
            weeks.add(week);
        }
        template.setWeeks(weeks);

        employee = new User();
        employee.setId(9L);
    }

    @Test
    void saturdayCronAssignsWeekOneOnTheCycleMonthsFirstSaturday() {
        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(template));
        when(assignmentRepository.findByTemplate_Id(11L)).thenReturn(Optional.empty());
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));

        scheduler.runAssignment(LocalDate.of(2026, 8, 1), "cron");

        ArgumentCaptor<WeeklyAccountabilityAssignment> captor = ArgumentCaptor.forClass(WeeklyAccountabilityAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertEquals(1, captor.getValue().getWeekNumber());
        assertEquals("cron", captor.getValue().getTriggerSource());
    }

    @Test
    void assignmentAdvancesToWeekTwoOnTheFollowingSaturday() {
        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(template));
        WeeklyAccountabilityAssignment existing = new WeeklyAccountabilityAssignment();
        existing.setTemplate(template);
        existing.setWeekNumber(1);
        when(assignmentRepository.findByTemplate_Id(11L)).thenReturn(Optional.of(existing));
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));

        scheduler.runAssignment(LocalDate.of(2026, 8, 8), "cron");

        ArgumentCaptor<WeeklyAccountabilityAssignment> captor = ArgumentCaptor.forClass(WeeklyAccountabilityAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertEquals(2, captor.getValue().getWeekNumber());
    }

    @Test
    void startupCatchUpTagsTriggerSourceAndReusesTheSameAssignmentPath() {
        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(template));
        when(assignmentRepository.findByTemplate_Id(11L)).thenReturn(Optional.empty());
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));

        scheduler.runStartupCatchUp();

        ArgumentCaptor<WeeklyAccountabilityAssignment> captor = ArgumentCaptor.forClass(WeeklyAccountabilityAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertEquals("startup-catch-up", captor.getValue().getTriggerSource());
    }

    @Test
    void periodicSafetyNetIsANoOpWhenTheAssignmentIsAlreadyUpToDate() {
        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(template));
        WeeklyAccountabilityAssignment existing = new WeeklyAccountabilityAssignment();
        existing.setTemplate(template);
        existing.setWeekNumber(weeklyAccountabilityTemplateService.computeSaturdayWeekNumber(template.getCycleMonth(), LocalDate.now()));
        when(assignmentRepository.findByTemplate_Id(11L)).thenReturn(Optional.of(existing));
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));

        scheduler.runPeriodicSafetyNet();

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void skipsGracefullyBeforeTheCycleMonthsFirstSaturday() {
        WeeklyAccountabilityTemplate septemberTemplate = new WeeklyAccountabilityTemplate();
        septemberTemplate.setId(12L);
        septemberTemplate.setName("September Template");
        septemberTemplate.setStatus(BundleStatus.ACTIVE);
        septemberTemplate.setTargetRole(template.getTargetRole());
        septemberTemplate.setCycleMonth(LocalDate.of(2026, 9, 1)); // a Tuesday - first Saturday is Sept 5
        septemberTemplate.setWeeks(template.getWeeks());

        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(septemberTemplate));

        WeeklyAccountabilityAssignmentSchedulerService.RunSummary summary =
                scheduler.runAssignment(LocalDate.of(2026, 9, 3), "cron");

        assertEquals(1, summary.getTemplatesSkippedBeforeFirstSaturday());
        assertEquals(0, summary.getWeeksAssigned());
        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void skipsGracefullyWhenTheAssignedWeekHasNoQuestions() {
        WeeklyAccountabilityWeek unscheduledWeek = new WeeklyAccountabilityWeek();
        unscheduledWeek.setWeekNumber(1);
        unscheduledWeek.setQuestions(new ArrayList<>());
        template.setWeeks(new ArrayList<>(List.of(unscheduledWeek)));

        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(template));

        WeeklyAccountabilityAssignmentSchedulerService.RunSummary summary =
                scheduler.runAssignment(LocalDate.of(2026, 8, 1), "cron");

        assertEquals(1, summary.getTemplatesSkippedUnscheduled());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void nonAlignedMonthAssignsOnTheSameCadenceAsAnAlignedOne() {
        // September 2026 starts on a Tuesday - day-range boundaries (1-7/8-14/...) do NOT line
        // up with its Saturdays (5, 12, 19, 26), unlike August 2026. Proves the cron's output
        // for a "week 2" run is identical in shape regardless of that coincidence.
        WeeklyAccountabilityTemplate septemberTemplate = new WeeklyAccountabilityTemplate();
        septemberTemplate.setId(12L);
        septemberTemplate.setName("September Template");
        septemberTemplate.setStatus(BundleStatus.ACTIVE);
        septemberTemplate.setTargetRole(template.getTargetRole());
        septemberTemplate.setCycleMonth(LocalDate.of(2026, 9, 1));
        septemberTemplate.setWeeks(template.getWeeks());

        when(templateRepository.findByStatus(BundleStatus.ACTIVE)).thenReturn(List.of(septemberTemplate));
        when(assignmentRepository.findByTemplate_Id(12L)).thenReturn(Optional.empty());
        when(userRepository.findByRoleIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(List.of(employee));

        scheduler.runAssignment(LocalDate.of(2026, 9, 12), "cron"); // Sept 12 is the month's 2nd Saturday

        ArgumentCaptor<WeeklyAccountabilityAssignment> captor = ArgumentCaptor.forClass(WeeklyAccountabilityAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertEquals(2, captor.getValue().getWeekNumber());
    }
}
