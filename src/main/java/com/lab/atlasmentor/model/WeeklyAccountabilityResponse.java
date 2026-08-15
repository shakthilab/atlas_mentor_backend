package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One employee's free-text answer to one {@link WeeklyAccountabilityQuestion} on one checkpoint
 * date. Carries an explicit {@link #week} FK rather than re-deriving the owning week from
 * {@link #checkpointDate} on every read - more explicit, and avoids recomputing the day-range
 * lookup (and depending on the week's day range never having changed since) each time a
 * historical answer is displayed. Free-text only: no status, no approval, no attachment - fully
 * separate from the task approval chain.
 */
@Entity
@Table(name = "weekly_accountability_responses",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"employee_user_id", "question_id", "checkpoint_date"},
                              name = "uk_weekly_acct_responses_employee_question_date")
       },
       indexes = {
           @Index(name = "idx_weekly_acct_responses_employee", columnList = "employee_user_id"),
           @Index(name = "idx_weekly_acct_responses_week", columnList = "week_id"),
           @Index(name = "idx_weekly_acct_responses_question", columnList = "question_id"),
           @Index(name = "idx_weekly_acct_responses_checkpoint_date", columnList = "checkpoint_date")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkpoint_date", nullable = false)
    private LocalDate checkpointDate;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    @JsonIgnoreProperties({"questions", "template"})
    private WeeklyAccountabilityWeek week;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private WeeklyAccountabilityQuestion question;

    public WeeklyAccountabilityResponse() {}
}
