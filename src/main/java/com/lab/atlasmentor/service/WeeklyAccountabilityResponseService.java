package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityAnswerResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityResponseSubmitRequest;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import com.lab.atlasmentor.model.WeeklyAccountabilityResponse;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Employee-facing submit/read of Weekly Accountability answers. Entirely separate from the
 * task approval chain - no status, no approval step, just an upsertable free-text answer per
 * (employee, question, checkpointDate).
 *
 * <p><b>Not gated by {@code DayWorkspace.isWeeklyCheckpointDay}</b> (the Role Template day-tree
 * flag {@link TemplateInstantiationService} derives per employee per day) - that mechanism is
 * a distinct, unrelated feature (which Role Template days are "checkpoint" days) and this
 * redesign deliberately decouples Weekly Accountability from it entirely. Instead, a
 * checkpoint date is valid here based on whatever
 * {@link WeeklyAccountabilityTemplateService#getCurrentAssignedWeek} reports: does the caller's
 * role have an ACTIVE template for the checkpoint date's cycle month, has the Saturday
 * assignment cron actually assigned one of that template's 4 weeks yet, and does that week
 * actually have questions scheduled. The per-week day ranges authored on the template are
 * display/record-keeping only here, not part of this resolution.
 *
 * <p><b>due_weekday is a display/reminder label only</b>, not an enforced deadline: this
 * service never rejects a submission for being "late" relative to a week's due_weekday, and
 * never allows answering a future week's questions early - checkpointDate must both equal
 * today (same-day cutoff, unchanged from before) AND fall within whichever week today's date
 * resolves to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityResponseService {

    private final UserRepository userRepository;
    private final WeeklyAccountabilityResponseRepository responseRepository;
    private final WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;

    /**
     * Same-day cutoff: a checkpoint's answers can only be submitted/edited on the checkpoint
     * date itself (LocalDate.now() at call time). Once that date has passed, the row is
     * effectively locked - re-POSTing is rejected rather than silently editing history, and
     * there is no separate "edit" endpoint since the same upsert-by-(employee,question,date)
     * logic serves both first submission and same-day correction.
     */
    @Transactional
    public List<WeeklyAccountabilityAnswerResponse> submitResponses(Long employeeId, WeeklyAccountabilityResponseSubmitRequest request) {
        LocalDate checkpointDate = request.getCheckpointDate();
        log.info("Employee {} submitting weekly accountability responses for {}", employeeId, checkpointDate);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (!LocalDate.now().equals(checkpointDate)) {
            throw new ValidationException("Weekly accountability responses can only be submitted or edited on the "
                    + "checkpoint date itself (today is " + LocalDate.now() + ", requested " + checkpointDate + ").");
        }

        Long roleId = employee.getRole() != null ? employee.getRole().getId() : null;
        LocalDate cycleMonth = checkpointDate.withDayOfMonth(1);
        WeeklyAccountabilityTemplate template = weeklyAccountabilityTemplateService
                .findActiveTemplateForRoleAndMonth(roleId, cycleMonth)
                .orElseThrow(() -> new ValidationException("No active weekly accountability template found for this role for "
                        + cycleMonth.getMonth() + " " + cycleMonth.getYear() + "."));

        WeeklyAccountabilityWeek week = weeklyAccountabilityTemplateService.getCurrentAssignedWeek(template)
                .orElseThrow(() -> new ValidationException("No week has been assigned yet for the active weekly accountability "
                        + "template - the Saturday assignment run hasn't happened for this cycle yet."));

        if (!week.isScheduled()) {
            throw new ValidationException("Week " + week.getWeekNumber() + " (" + week.getDayRangeStart() + "-"
                    + week.getDayRangeEnd() + ") has no questions scheduled for " + checkpointDate + ".");
        }

        Map<Long, WeeklyAccountabilityQuestion> validQuestions = week.getQuestions().stream()
                .collect(Collectors.toMap(WeeklyAccountabilityQuestion::getId, q -> q));

        LocalDateTime now = LocalDateTime.now();
        for (WeeklyAccountabilityAnswerRequest answerReq : request.getAnswers()) {
            WeeklyAccountabilityQuestion question = validQuestions.get(answerReq.getQuestionId());
            if (question == null) {
                throw new ValidationException("Question ID " + answerReq.getQuestionId()
                        + " does not belong to week " + week.getWeekNumber() + " of the active weekly accountability template for this role.");
            }

            WeeklyAccountabilityResponse response = responseRepository
                    .findByEmployeeIdAndQuestionIdAndCheckpointDate(employeeId, question.getId(), checkpointDate)
                    .orElseGet(() -> {
                        WeeklyAccountabilityResponse newResponse = new WeeklyAccountabilityResponse();
                        newResponse.setEmployee(employee);
                        newResponse.setWeek(week);
                        newResponse.setQuestion(question);
                        newResponse.setCheckpointDate(checkpointDate);
                        newResponse.setCreatedBy(employeeId);
                        return newResponse;
                    });

            response.setAnswerText(answerReq.getAnswerText());
            response.setAnsweredAt(now);
            response.setUpdatedBy(employeeId);
            responseRepository.save(response);
        }

        return getResponses(employeeId, checkpointDate);
    }

    @Transactional(readOnly = true)
    public List<WeeklyAccountabilityAnswerResponse> getResponses(Long employeeId, LocalDate checkpointDate) {
        log.info("Fetching weekly accountability responses for employee {} on {}", employeeId, checkpointDate);
        return responseRepository.findByEmployeeIdAndCheckpointDate(employeeId, checkpointDate).stream()
                .map(this::convertToResponse)
                .sorted(Comparator.comparing(a -> Optional.ofNullable(a.getQuestionId()).orElse(0L)))
                .collect(Collectors.toList());
    }

    private WeeklyAccountabilityAnswerResponse convertToResponse(WeeklyAccountabilityResponse response) {
        WeeklyAccountabilityAnswerResponse dto = new WeeklyAccountabilityAnswerResponse();
        dto.setId(response.getId());
        dto.setCheckpointDate(response.getCheckpointDate());
        dto.setAnswerText(response.getAnswerText());
        dto.setAnsweredAt(response.getAnsweredAt());
        if (response.getWeek() != null) {
            dto.setWeekNumber(response.getWeek().getWeekNumber());
        }
        if (response.getQuestion() != null) {
            dto.setQuestionId(response.getQuestion().getId());
            dto.setQuestionText(response.getQuestion().getQuestionText());
        }
        return dto;
    }
}
