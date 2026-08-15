package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.WeeklyAccountabilityMyTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityQuestionRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityWeekRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityWeekResponse;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.exception.ConflictException;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityAssignment;
import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.RoleRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityAssignmentRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyAccountabilityTemplateServiceTest {

    @Mock
    private WeeklyAccountabilityTemplateRepository templateRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyAccountabilityAssignmentRepository assignmentRepository;

    @InjectMocks
    private WeeklyAccountabilityTemplateService service;

    private Role role() {
        Role role = new Role();
        role.setId(4L);
        role.setName("SENIOR_COUNSELLOR");
        role.setDisplayName("Senior Counsellor");
        return role;
    }

    private WeeklyAccountabilityTemplateRequest requestFor(String cycleMonth, List<WeeklyAccountabilityWeekRequest> weeks) {
        WeeklyAccountabilityTemplateRequest request = new WeeklyAccountabilityTemplateRequest();
        request.setName("Senior Branch Lead Weekly Audit");
        request.setRoleId(4L);
        request.setCycleMonth(cycleMonth);
        request.setWeeks(weeks);
        return request;
    }

    private WeeklyAccountabilityWeekRequest week(int weekNumber, DayOfWeek dueWeekday, String... questionTexts) {
        WeeklyAccountabilityWeekRequest week = new WeeklyAccountabilityWeekRequest();
        week.setWeekNumber(weekNumber);
        week.setDueWeekday(dueWeekday);
        List<WeeklyAccountabilityQuestionRequest> questions = new ArrayList<>();
        for (String text : questionTexts) {
            WeeklyAccountabilityQuestionRequest q = new WeeklyAccountabilityQuestionRequest();
            q.setQuestionText(text);
            questions.add(q);
        }
        week.setQuestions(questions);
        return week;
    }

    @Test
    void createTemplateAlwaysCreatesAllFourWeeksWithServerComputedDayRanges() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only Week 1 is authored in the request - Weeks 2-4 must still be created, unscheduled.
        WeeklyAccountabilityTemplateRequest request = requestFor("2026-08",
                List.of(week(1, DayOfWeek.FRIDAY, "Week 1 performance question")));

        WeeklyAccountabilityTemplateResponse response = service.createTemplate(request, 1L);

        assertEquals(LocalDate.of(2026, 8, 1), response.getCycleMonth());
        assertEquals(BundleStatus.DRAFT, response.getStatus());
        assertEquals(4, response.getWeeks().size());

        WeeklyAccountabilityWeekResponse week1 = response.getWeeks().get(0);
        assertEquals(1, week1.getDayRangeStart());
        assertEquals(7, week1.getDayRangeEnd());
        assertTrue(week1.isScheduled());
        assertEquals(1, week1.getQuestions().size());

        WeeklyAccountabilityWeekResponse week2 = response.getWeeks().get(1);
        assertEquals(8, week2.getDayRangeStart());
        assertEquals(14, week2.getDayRangeEnd());
        assertFalse(week2.isScheduled());

        WeeklyAccountabilityWeekResponse week4 = response.getWeeks().get(3);
        // August has 31 days - week 4 extends 22-31, not a fixed 22-28.
        assertEquals(22, week4.getDayRangeStart());
        assertEquals(31, week4.getDayRangeEnd());
    }

    @Test
    void createTemplateFebruaryWeek4StopsAtActualMonthEnd() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        WeeklyAccountabilityTemplateResponse response = service.createTemplate(requestFor("2026-02", null), 1L);

        WeeklyAccountabilityWeekResponse week4 = response.getWeeks().get(3);
        assertEquals(22, week4.getDayRangeStart());
        assertEquals(28, week4.getDayRangeEnd()); // 2026 is not a leap year
    }

    @Test
    void createTemplateRejectsMalformedCycleMonth() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        assertThrows(ValidationException.class, () -> service.createTemplate(requestFor("not-a-month", null), 1L));
    }

    @Test
    void publishSucceedsWithOnlyOneWeekScheduled() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        WeeklyAccountabilityTemplateResponse created = service.createTemplate(
                requestFor("2026-08", List.of(week(1, DayOfWeek.FRIDAY, "Q1"))), 1L);

        WeeklyAccountabilityTemplate persisted = toEntity(created);
        when(templateRepository.findById(created.getId())).thenReturn(Optional.of(persisted));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, persisted.getCycleMonth(), BundleStatus.ACTIVE))
                .thenReturn(Optional.empty());

        WeeklyAccountabilityTemplateResponse published = service.publishTemplate(created.getId(), 1L);

        assertEquals(BundleStatus.ACTIVE, published.getStatus());
    }

    @Test
    void publishFailsWhenNoWeekHasAnyQuestion() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        WeeklyAccountabilityTemplateResponse created = service.createTemplate(requestFor("2026-08", null), 1L);

        WeeklyAccountabilityTemplate persisted = toEntity(created);
        when(templateRepository.findById(created.getId())).thenReturn(Optional.of(persisted));

        assertThrows(ValidationException.class, () -> service.publishTemplate(created.getId(), 1L));
    }

    @Test
    void publishFailsWith409WhenRoleAlreadyHasActiveTemplateForSameCycleMonth() {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setTargetRole(role());
        template.setCycleMonth(LocalDate.of(2026, 8, 1));
        template.setStatus(BundleStatus.DRAFT);
        WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
        week.setWeekNumber(1);
        week.setQuestions(List.of(new WeeklyAccountabilityQuestion()));
        template.setWeeks(List.of(week));

        WeeklyAccountabilityTemplate alreadyActive = new WeeklyAccountabilityTemplate();
        alreadyActive.setId(99L);

        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, LocalDate.of(2026, 8, 1), BundleStatus.ACTIVE))
                .thenReturn(Optional.of(alreadyActive));

        assertThrows(ConflictException.class, () -> service.publishTemplate(7L, 1L));
    }

    @Test
    void duplicateWeekReplacesTargetWeekQuestionsRatherThanAppending() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        WeeklyAccountabilityTemplateRequest request = requestFor("2026-08", List.of(
                week(1, DayOfWeek.FRIDAY, "Week 1 Q1", "Week 1 Q2"),
                week(2, DayOfWeek.FRIDAY, "Pre-existing Week 2 question")));
        WeeklyAccountabilityTemplateResponse created = service.createTemplate(request, 1L);

        WeeklyAccountabilityTemplate persisted = toEntity(created);
        when(templateRepository.findById(created.getId())).thenReturn(Optional.of(persisted));

        WeeklyAccountabilityTemplateResponse result = service.duplicateWeek(created.getId(), 2, 1, 1L);

        WeeklyAccountabilityWeekResponse week2 = result.getWeeks().get(1);
        assertEquals(2, week2.getQuestions().size());
        assertEquals("Week 1 Q1", week2.getQuestions().get(0).getQuestionText());
        assertEquals("Week 1 Q2", week2.getQuestions().get(1).getQuestionText());
        // The pre-existing Week 2 question must be gone, not appended alongside the copies.
        assertTrue(week2.getQuestions().stream().noneMatch(q -> "Pre-existing Week 2 question".equals(q.getQuestionText())));
    }

    @Test
    void updateTemplateStatusDeactivatesAndReactivatesAnAlreadyPublishedTemplate() {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setTargetRole(role());
        template.setCycleMonth(LocalDate.of(2026, 8, 1));
        template.setStatus(BundleStatus.ACTIVE);
        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        WeeklyAccountabilityTemplateResponse deactivated = service.updateTemplateStatus(7L, BundleStatus.INACTIVE, 1L);
        assertEquals(BundleStatus.INACTIVE, deactivated.getStatus());

        // Reactivating checks for a conflicting ACTIVE template the same way publish does.
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, LocalDate.of(2026, 8, 1), BundleStatus.ACTIVE))
                .thenReturn(Optional.empty());
        WeeklyAccountabilityTemplateResponse reactivated = service.updateTemplateStatus(7L, BundleStatus.ACTIVE, 1L);
        assertEquals(BundleStatus.ACTIVE, reactivated.getStatus());
    }

    @Test
    void updateTemplateStatusRejects409OnReactivateWhenAnotherTemplateTookTheSlot() {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setTargetRole(role());
        template.setCycleMonth(LocalDate.of(2026, 8, 1));
        template.setStatus(BundleStatus.INACTIVE);

        WeeklyAccountabilityTemplate anotherActive = new WeeklyAccountabilityTemplate();
        anotherActive.setId(99L);

        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, LocalDate.of(2026, 8, 1), BundleStatus.ACTIVE))
                .thenReturn(Optional.of(anotherActive));

        assertThrows(ConflictException.class, () -> service.updateTemplateStatus(7L, BundleStatus.ACTIVE, 1L));
    }

    @Test
    void updateTemplateStatusRejectsSettingDraftDirectly() {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setStatus(BundleStatus.ACTIVE);
        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));

        assertThrows(ValidationException.class, () -> service.updateTemplateStatus(7L, BundleStatus.DRAFT, 1L));
    }

    @Test
    void updateTemplateStatusRejectsChangingStatusWhileStillDraft() {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setStatus(BundleStatus.DRAFT);
        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));

        assertThrows(ValidationException.class, () -> service.updateTemplateStatus(7L, BundleStatus.INACTIVE, 1L));
    }

    @Test
    void deleteTemplateAllowsInactiveButBlocksActive() {
        WeeklyAccountabilityTemplate inactive = new WeeklyAccountabilityTemplate();
        inactive.setId(7L);
        inactive.setStatus(BundleStatus.INACTIVE);
        when(templateRepository.findById(7L)).thenReturn(Optional.of(inactive));

        service.deleteTemplate(7L);
        verify(templateRepository).delete(inactive);

        WeeklyAccountabilityTemplate active = new WeeklyAccountabilityTemplate();
        active.setId(8L);
        active.setStatus(BundleStatus.ACTIVE);
        when(templateRepository.findById(8L)).thenReturn(Optional.of(active));

        assertThrows(ConflictException.class, () -> service.deleteTemplate(8L));
    }

    @Test
    void duplicateTemplateCopiesAllWeeksAsANewIndependentDraftTemplate() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        WeeklyAccountabilityTemplateRequest request = requestFor("2026-08", List.of(
                week(1, DayOfWeek.FRIDAY, "Week 1 Q1"),
                week(2, DayOfWeek.FRIDAY, "Week 2 Q1", "Week 2 Q2")));
        WeeklyAccountabilityTemplateResponse source = service.createTemplate(request, 1L);
        source.setStatus(BundleStatus.ACTIVE); // simulate an already-published source

        WeeklyAccountabilityTemplate persisted = toEntity(source);
        persisted.setStatus(BundleStatus.ACTIVE);
        when(templateRepository.findById(source.getId())).thenReturn(Optional.of(persisted));

        com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest duplicateRequest =
                new com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest();
        // No newTemplateName / newCycleMonth given - both should default.

        WeeklyAccountabilityTemplateResponse copy = service.duplicateTemplate(source.getId(), duplicateRequest, 2L);

        assertEquals("Senior Branch Lead Weekly Audit Copy", copy.getName());
        assertEquals(BundleStatus.DRAFT, copy.getStatus()); // always DRAFT, even though source was ACTIVE
        assertEquals(source.getCycleMonth(), copy.getCycleMonth());
        assertEquals(4, copy.getWeeks().size());
        assertEquals(1, copy.getWeeks().get(0).getQuestions().size());
        assertEquals("Week 1 Q1", copy.getWeeks().get(0).getQuestions().get(0).getQuestionText());
        assertEquals(2, copy.getWeeks().get(1).getQuestions().size());
        assertFalse(copy.getWeeks().get(2).isScheduled());
    }

    @Test
    void duplicateTemplateRecomputesDayRangesForANewTargetCycleMonth() {
        when(roleRepository.findById(4L)).thenReturn(Optional.of(role()));
        when(templateRepository.save(any(WeeklyAccountabilityTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        // Source is a 28-day February template.
        WeeklyAccountabilityTemplateResponse source = service.createTemplate(requestFor("2026-02", null), 1L);
        WeeklyAccountabilityTemplate persisted = toEntity(source);
        when(templateRepository.findById(source.getId())).thenReturn(Optional.of(persisted));

        com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest duplicateRequest =
                new com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest();
        duplicateRequest.setNewCycleMonth("2026-08"); // target a 31-day month instead

        WeeklyAccountabilityTemplateResponse copy = service.duplicateTemplate(source.getId(), duplicateRequest, 2L);

        assertEquals(LocalDate.of(2026, 8, 1), copy.getCycleMonth());
        WeeklyAccountabilityWeekResponse week4 = copy.getWeeks().get(3);
        assertEquals(22, week4.getDayRangeStart());
        assertEquals(31, week4.getDayRangeEnd()); // not the source's 28 - re-derived for August
    }

    /** Builds an assignment row (as the Saturday cron would have persisted it) pointing at the given template + week number. */
    private WeeklyAccountabilityAssignment assignmentFor(WeeklyAccountabilityTemplate template, int weekNumber) {
        WeeklyAccountabilityAssignment assignment = new WeeklyAccountabilityAssignment();
        assignment.setTemplate(template);
        assignment.setWeekNumber(weekNumber);
        assignment.setCycleMonth(template.getCycleMonth());
        assignment.setAssignedAt(java.time.LocalDateTime.now());
        assignment.setTriggerSource("cron");
        return assignment;
    }

    @Test
    void getMyTemplateReturnsOnlyTheCronAssignedWeeksQuestions() {
        Role role = role();
        User employee = new User();
        employee.setId(100L);
        employee.setRole(role);

        LocalDate today = LocalDate.now();
        LocalDate cycleMonth = today.withDayOfMonth(1);

        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setName("Senior Branch Lead Weekly Audit");
        template.setTargetRole(role);
        template.setCycleMonth(cycleMonth);
        template.setStatus(BundleStatus.ACTIVE);

        // Assigned week's own day-range is irrelevant to resolution now - it's read-only
        // display data, so it's set to something that would NOT contain today's date, to prove
        // getMyTemplate no longer recomputes off day-range/today at all.
        WeeklyAccountabilityWeek assignedWeek = new WeeklyAccountabilityWeek();
        assignedWeek.setWeekNumber(2);
        assignedWeek.setDayRangeStart(8);
        assignedWeek.setDayRangeEnd(14);
        assignedWeek.setDueWeekday(DayOfWeek.FRIDAY);
        WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
        question.setId(55L);
        question.setQuestionText("How many students onboarded this week?");
        question.setDisplayOrder(0);
        assignedWeek.setQuestions(new ArrayList<>(List.of(question)));

        WeeklyAccountabilityWeek otherWeek = new WeeklyAccountabilityWeek();
        otherWeek.setWeekNumber(1);
        otherWeek.setDayRangeStart(1);
        otherWeek.setDayRangeEnd(7);

        template.setWeeks(new ArrayList<>(List.of(otherWeek, assignedWeek)));

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, cycleMonth, BundleStatus.ACTIVE))
                .thenReturn(Optional.of(template));
        when(assignmentRepository.findByTemplate_Id(7L)).thenReturn(Optional.of(assignmentFor(template, 2)));

        WeeklyAccountabilityMyTemplateResponse result = service.getMyTemplate(100L);

        assertNotNull(result);
        assertEquals(2, result.getWeekNumber());
        assertEquals(1, result.getQuestions().size());
        assertEquals("How many students onboarded this week?", result.getQuestions().get(0).getQuestionText());
    }

    @Test
    void getMyTemplateReturnsNullWhenNoActiveTemplateForRoleThisMonth() {
        Role role = role();
        User employee = new User();
        employee.setId(100L);
        employee.setRole(role);

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(eq(4L), any(LocalDate.class), eq(BundleStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        assertNull(service.getMyTemplate(100L));
    }

    @Test
    void getMyTemplateReturnsNullWhenNoSaturdayAssignmentRunHasHappenedYet() {
        Role role = role();
        User employee = new User();
        employee.setId(100L);
        employee.setRole(role);

        LocalDate today = LocalDate.now();
        LocalDate cycleMonth = today.withDayOfMonth(1);

        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setTargetRole(role);
        template.setCycleMonth(cycleMonth);
        template.setStatus(BundleStatus.ACTIVE);
        template.setWeeks(new ArrayList<>());

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, cycleMonth, BundleStatus.ACTIVE))
                .thenReturn(Optional.of(template));
        when(assignmentRepository.findByTemplate_Id(7L)).thenReturn(Optional.empty());

        // No assignment row yet (e.g. published after this month's Saturdays already passed,
        // or before the first one) - not an error, just nothing to answer right now.
        assertNull(service.getMyTemplate(100L));
    }

    @Test
    void getMyTemplateReturnsNullWhenCronAssignedWeekIsUnscheduled() {
        Role role = role();
        User employee = new User();
        employee.setId(100L);
        employee.setRole(role);

        LocalDate today = LocalDate.now();
        LocalDate cycleMonth = today.withDayOfMonth(1);

        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);
        template.setTargetRole(role);
        template.setCycleMonth(cycleMonth);
        template.setStatus(BundleStatus.ACTIVE);

        WeeklyAccountabilityWeek assignedWeek = new WeeklyAccountabilityWeek();
        assignedWeek.setWeekNumber(1);
        assignedWeek.setDayRangeStart(1);
        assignedWeek.setDayRangeEnd(7);
        assignedWeek.setQuestions(new ArrayList<>()); // unscheduled
        template.setWeeks(new ArrayList<>(List.of(assignedWeek)));

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(4L, cycleMonth, BundleStatus.ACTIVE))
                .thenReturn(Optional.of(template));
        when(assignmentRepository.findByTemplate_Id(7L)).thenReturn(Optional.of(assignmentFor(template, 1)));

        assertNull(service.getMyTemplate(100L));
    }

    @Test
    void getMyTemplateThrowsResourceNotFoundWhenEmployeeDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMyTemplate(999L));
    }

    // ===== computeSaturdayWeekNumber: the arithmetic the assignment cron uses to decide which
    // week a Saturday trigger corresponds to. Deliberately exercised against a month that starts
    // on a Saturday (August 2026 - the coincidental-alignment case) AND one that doesn't
    // (September 2026, starts on a Tuesday) to prove the cadence is identical either way. =====

    @Test
    void computeSaturdayWeekNumberAdvancesOneWeekPerSaturdayWhenMonthStartsOnSaturday() {
        LocalDate august2026 = LocalDate.of(2026, 8, 1); // a Saturday
        assertEquals(1, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 1)));
        assertEquals(1, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 5))); // mid-week, still week 1
        assertEquals(2, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 8)));
        assertEquals(3, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 15)));
        assertEquals(4, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 22)));
        // A 5th Saturday (Aug 29) re-confirms week 4 rather than overflowing to 5.
        assertEquals(4, service.computeSaturdayWeekNumber(august2026, LocalDate.of(2026, 8, 29)));
    }

    @Test
    void computeSaturdayWeekNumberWorksIdenticallyWhenMonthDoesNotStartOnSaturday() {
        LocalDate september2026 = LocalDate.of(2026, 9, 1); // a Tuesday - day-range boundaries (1-7/8-14/...) do NOT line up with Saturdays here
        // Before the month's first Saturday (Sept 5): nothing assigned yet.
        assertEquals(0, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 1)));
        assertEquals(0, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 4)));
        assertEquals(1, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 5)));
        // Sept 8 falls inside week 2's day-range (8-14) under the OLD live-recompute model, but
        // the 2nd Saturday hasn't happened yet (that's Sept 12) - still week 1 under the new model.
        assertEquals(1, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 8)));
        assertEquals(2, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 12)));
        assertEquals(3, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 19)));
        assertEquals(4, service.computeSaturdayWeekNumber(september2026, LocalDate.of(2026, 9, 26)));
    }

    /** Round-trips a saved response DTO back into a mock-repository-friendly entity graph. */
    private WeeklyAccountabilityTemplate toEntity(WeeklyAccountabilityTemplateResponse response) {
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(response.getId() != null ? response.getId() : 7L);
        template.setName(response.getName());
        template.setTargetRole(role());
        template.setCycleMonth(response.getCycleMonth());
        template.setStatus(response.getStatus());
        List<WeeklyAccountabilityWeek> weeks = new ArrayList<>();
        for (WeeklyAccountabilityWeekResponse wr : response.getWeeks()) {
            WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
            week.setId(wr.getId());
            week.setWeekNumber(wr.getWeekNumber());
            week.setDayRangeStart(wr.getDayRangeStart());
            week.setDayRangeEnd(wr.getDayRangeEnd());
            week.setDueWeekday(wr.getDueWeekday());
            week.setTemplate(template);
            List<WeeklyAccountabilityQuestion> questions = new ArrayList<>();
            long qId = 1000L + wr.getWeekNumber() * 10L;
            for (var qr : wr.getQuestions()) {
                WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
                question.setId(qId++);
                question.setQuestionText(qr.getQuestionText());
                question.setDisplayOrder(qr.getDisplayOrder());
                question.setWeek(week);
                questions.add(question);
            }
            week.setQuestions(questions);
            weeks.add(week);
        }
        template.setWeeks(weeks);
        return template;
    }
}
