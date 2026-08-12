package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Publish-time caller of {@link TemplateInstantiationService}. Runs immediately after a
 * template flips to ACTIVE so an employee's Day 1 isn't wrongly delayed until the next
 * midnight cron.
 *
 * Only today gets instantiated - never earlier days. This isn't a special case coded here;
 * it falls out of always calling {@link TemplateInstantiationService#instantiateDayFor} with
 * today's date, which is the exact same method {@link TemplateInstantiationSchedulerService}
 * calls every night. If the cron already ran (or a concurrent publish already did), the
 * shared idempotency check in that method makes tonight's cron run a no-op for anyone
 * instantiated here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplatePublishService {

    private final UserRepository userRepository;
    private final TemplateInstantiationService templateInstantiationService;

    public void instantiateFor(TaskBundle template) {
        LocalDate today = LocalDate.now();
        log.info("Publish-time instantiation for task bundle: {} (ID: {}), date: {}",
                template.getName(), template.getId(), today);

        if (template.getRole() == null) {
            log.warn("Template {} ({}) has no target role, nothing to instantiate on publish",
                    template.getId(), template.getName());
            return;
        }

        List<User> employees = userRepository.findByRoleIdAndStatus(template.getRole().getId(), UserStatus.ACTIVE);

        int created = 0;
        int alreadyInstantiated = 0;
        int exhausted = 0;
        int errors = 0;

        for (User employee : employees) {
            try {
                TemplateInstantiationService.InstantiationResult result =
                        templateInstantiationService.instantiateDayFor(template, employee, today, "publish");
                switch (result.getOutcome()) {
                    case CREATED -> created += result.getTasksCreated();
                    case ALREADY_INSTANTIATED -> alreadyInstantiated++;
                    case TEMPLATE_EXHAUSTED -> exhausted++;
                }
            } catch (Exception e) {
                errors++;
                log.error("Error instantiating template {} today for employee {}: {}",
                        template.getId(), employee.getId(), e.getMessage(), e);
            }
        }

        log.info("Publish-time instantiation complete for template {} ({}): employees={}, tasksCreated={}, "
                        + "alreadyInstantiated={}, templateExhausted={}, errors={}",
                template.getId(), template.getName(), employees.size(), created, alreadyInstantiated, exhausted, errors);
    }
}
