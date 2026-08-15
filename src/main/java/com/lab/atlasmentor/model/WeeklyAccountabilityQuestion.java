package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single ordered question on a {@link WeeklyAccountabilityWeek} (not the template directly -
 * each of a template's 4 weeks owns its own independent question list). Deliberately minimal
 * compared to {@link com.lab.atlasmentor.model.TemplateTask} - no priority/status, since these
 * aren't trackable work items, just free-text prompts.
 */
@Entity
@Table(name = "weekly_accountability_questions",
       indexes = {
           @Index(name = "idx_weekly_acct_questions_week", columnList = "week_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    @JsonIgnoreProperties({"questions"})
    private WeeklyAccountabilityWeek week;

    public WeeklyAccountabilityQuestion() {}
}
