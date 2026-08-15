package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.DayOfWeek;
import java.util.List;

/**
 * One of a {@link WeeklyAccountabilityTemplate}'s 4 fixed week slots (week_number 1-4), each
 * independently configured: its own {@link #dueWeekday} (display/reminder text only - see
 * {@link com.lab.atlasmentor.service.WeeklyAccountabilityResponseService}, not an enforced
 * cutoff) and its own {@link #questions}. {@link #dayRangeStart}/{@link #dayRangeEnd} are
 * computed once server-side when the owning template is created (weeks 1-3 are always 1-7/
 * 8-14/15-21; week 4 runs 22 through the actual last day of the template's cycle_month), so
 * every calendar day of the month belongs to exactly one week - no day-29-31 special case
 * needed anywhere that reads these columns.
 *
 * <p>{@link #isScheduled()} is deliberately NOT a stored column: "scheduled" is nothing more
 * than "this week has at least one question", and persisting it separately would let it drift
 * out of sync with the actual question count (e.g. the last question on a week gets deleted
 * but the flag doesn't flip back).
 */
@Entity
@Table(name = "weekly_accountability_weeks",
       indexes = {
           @Index(name = "idx_weekly_acct_weeks_template", columnList = "template_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityWeek extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "day_range_start", nullable = false)
    private Integer dayRangeStart;

    @Column(name = "day_range_end", nullable = false)
    private Integer dayRangeEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "due_weekday")
    private DayOfWeek dueWeekday;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnoreProperties({"weeks"})
    private WeeklyAccountabilityTemplate template;

    @OneToMany(mappedBy = "week", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"week"})
    private List<WeeklyAccountabilityQuestion> questions;

    public WeeklyAccountabilityWeek() {}

    public Long getTemplateId() {
        return template != null ? template.getId() : null;
    }

    public boolean isScheduled() {
        return questions != null && !questions.isEmpty();
    }

    /** Inclusive containment check against day-of-month, e.g. checkpointDate.getDayOfMonth(). */
    public boolean containsDayOfMonth(int dayOfMonth) {
        return dayRangeStart != null && dayRangeEnd != null
                && dayOfMonth >= dayRangeStart && dayOfMonth <= dayRangeEnd;
    }
}
