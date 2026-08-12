package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_accountability_responses",
       indexes = {
           @Index(name = "idx_weekly_responses_workspace", columnList = "day_workspace_id"),
           @Index(name = "idx_weekly_responses_employee", columnList = "employee_user_id"),
           @Index(name = "idx_weekly_responses_question", columnList = "question_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_workspace_id", nullable = false)
    @JsonIgnoreProperties({"tasks"})
    private DayWorkspace dayWorkspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private WeeklyAccountabilityQuestion question;

    public WeeklyAccountabilityResponse() {}
}
