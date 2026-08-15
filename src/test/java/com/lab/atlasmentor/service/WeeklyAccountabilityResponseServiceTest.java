package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityResponseSubmitRequest;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityResponseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyAccountabilityResponseServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyAccountabilityResponseRepository responseRepository;
    @Mock
    private WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;

    @InjectMocks
    private WeeklyAccountabilityResponseService service;

    private User employeeWithRole(long roleId) {
        Role role = new Role();
        role.setId(roleId);
        User employee = new User();
        employee.setId(100L);
        employee.setRole(role);
        return employee;
    }

    private WeeklyAccountabilityResponseSubmitRequest submitRequest(LocalDate checkpointDate, Long questionId, String answer) {
        WeeklyAccountabilityResponseSubmitRequest request = new WeeklyAccountabilityResponseSubmitRequest();
        request.setCheckpointDate(checkpointDate);
        WeeklyAccountabilityAnswerRequest answerReq = new WeeklyAccountabilityAnswerRequest();
        answerReq.setQuestionId(questionId);
        answerReq.setAnswerText(answer);
        request.setAnswers(List.of(answerReq));
        return request;
    }

    @Test
    void rejectsSubmissionForAnyDateOtherThanToday() {
        User employee = employeeWithRole(4L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));

        LocalDate notToday = LocalDate.now().minusDays(1);
        assertThrows(ValidationException.class,
                () -> service.submitResponses(100L, submitRequest(notToday, 55L, "answer")));
        verifyNoInteractions(weeklyAccountabilityTemplateService);
    }

    @Test
    void rejectsSubmissionWhenNoActiveTemplateForRoleThisMonth() {
        User employee = employeeWithRole(4L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        LocalDate today = LocalDate.now();
        when(weeklyAccountabilityTemplateService.findActiveTemplateForRoleAndMonth(4L, today.withDayOfMonth(1)))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.submitResponses(100L, submitRequest(today, 55L, "answer")));
    }

    @Test
    void rejectsSubmissionWhenNoSaturdayAssignmentRunHasHappenedYet() {
        User employee = employeeWithRole(4L);
        LocalDate today = LocalDate.now();
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(weeklyAccountabilityTemplateService.findActiveTemplateForRoleAndMonth(4L, today.withDayOfMonth(1)))
                .thenReturn(Optional.of(template));
        when(weeklyAccountabilityTemplateService.getCurrentAssignedWeek(template)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.submitResponses(100L, submitRequest(today, 55L, "answer")));
    }

    @Test
    void rejectsSubmissionWhenCurrentWeekIsUnscheduled() {
        User employee = employeeWithRole(4L);
        LocalDate today = LocalDate.now();
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);

        WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
        week.setWeekNumber(1);
        week.setDayRangeStart(1);
        week.setDayRangeEnd(31);
        week.setQuestions(new ArrayList<>()); // unscheduled

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(weeklyAccountabilityTemplateService.findActiveTemplateForRoleAndMonth(4L, today.withDayOfMonth(1)))
                .thenReturn(Optional.of(template));
        when(weeklyAccountabilityTemplateService.getCurrentAssignedWeek(template)).thenReturn(Optional.of(week));

        assertThrows(ValidationException.class,
                () -> service.submitResponses(100L, submitRequest(today, 55L, "answer")));
    }

    @Test
    void rejectsAnswerForQuestionNotBelongingToTheCurrentWeek() {
        User employee = employeeWithRole(4L);
        LocalDate today = LocalDate.now();
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);

        WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
        question.setId(55L);
        question.setQuestionText("Real question");

        WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
        week.setWeekNumber(1);
        week.setDayRangeStart(1);
        week.setDayRangeEnd(31);
        week.setQuestions(List.of(question));

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(weeklyAccountabilityTemplateService.findActiveTemplateForRoleAndMonth(4L, today.withDayOfMonth(1)))
                .thenReturn(Optional.of(template));
        when(weeklyAccountabilityTemplateService.getCurrentAssignedWeek(template)).thenReturn(Optional.of(week));

        // 999 doesn't belong to the resolved week's question set.
        assertThrows(ValidationException.class,
                () -> service.submitResponses(100L, submitRequest(today, 999L, "answer")));
    }

    @Test
    void submitsSuccessfullyAndStampsTheResolvedWeekOntoTheResponse() {
        User employee = employeeWithRole(4L);
        LocalDate today = LocalDate.now();
        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setId(7L);

        WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
        question.setId(55L);
        question.setQuestionText("How many students onboarded?");

        WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
        week.setWeekNumber(2);
        week.setDayRangeStart(1);
        week.setDayRangeEnd(31);
        week.setQuestions(List.of(question));

        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(weeklyAccountabilityTemplateService.findActiveTemplateForRoleAndMonth(4L, today.withDayOfMonth(1)))
                .thenReturn(Optional.of(template));
        when(weeklyAccountabilityTemplateService.getCurrentAssignedWeek(template)).thenReturn(Optional.of(week));
        when(responseRepository.findByEmployeeIdAndQuestionIdAndCheckpointDate(100L, 55L, today)).thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepository.findByEmployeeIdAndCheckpointDate(100L, today)).thenAnswer(inv -> {
            var saved = new com.lab.atlasmentor.model.WeeklyAccountabilityResponse();
            saved.setQuestion(question);
            saved.setWeek(week);
            saved.setCheckpointDate(today);
            saved.setAnswerText("42");
            return List.of(saved);
        });

        List<WeeklyAccountabilityAnswerResponse> result = service.submitResponses(100L, submitRequest(today, 55L, "42"));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getWeekNumber());
        assertEquals("42", result.get(0).getAnswerText());
    }
}
