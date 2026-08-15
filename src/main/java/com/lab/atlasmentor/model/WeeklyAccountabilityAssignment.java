package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The persisted output of one template's most recent Saturday assignment run - "which of the
 * 4 weeks is currently assigned" - written exclusively by
 * {@link com.lab.atlasmentor.service.WeeklyAccountabilityAssignmentSchedulerService}. Exactly
 * one row per {@link WeeklyAccountabilityTemplate} (see {@code uk_weekly_acct_assignments_template}):
 * a Saturday trigger updates this row in place rather than appending a new one each week, and
 * every employee under the template's role shares this single current assignment - there is no
 * per-employee fan-out here.
 *
 * <p>Read paths ({@code my-template}, submit) only ever fetch this row; none of them recompute
 * "current week" from today's date against {@link WeeklyAccountabilityWeek#getDayRangeStart()}/
 * {@link WeeklyAccountabilityWeek#getDayRangeEnd()} - those day-range fields remain for
 * display/record-keeping only.
 */
@Entity
@Table(name = "weekly_accountability_assignments")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"weeks"})
    private WeeklyAccountabilityTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    @JsonIgnoreProperties({"template"})
    private WeeklyAccountabilityWeek week;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    /** Always the 1st of the month - mirrors {@link WeeklyAccountabilityTemplate#getCycleMonth()}. */
    @Column(name = "cycle_month", nullable = false)
    private LocalDate cycleMonth;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    /** "cron" | "startup-catch-up" | "periodic-safety-net" - which trigger last touched this row. */
    @Column(name = "trigger_source", nullable = false, length = 30)
    private String triggerSource;

    public WeeklyAccountabilityAssignment() {}

    public Long getTemplateId() {
        return template != null ? template.getId() : null;
    }
}
