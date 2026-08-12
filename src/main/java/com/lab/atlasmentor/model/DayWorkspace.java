package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "day_workspaces",
       indexes = {
           @Index(name = "idx_day_workspaces_employee", columnList = "employee_user_id"),
           @Index(name = "idx_day_workspaces_branch", columnList = "branch_id"),
           @Index(name = "idx_day_workspaces_date", columnList = "work_date")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DayWorkspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_stage", nullable = false)
    private String approvalStage;

    @Column(name = "daily_completion_pct")
    private BigDecimal dailyCompletionPct;

    @Column(name = "day_number_in_month")
    private Integer dayNumberInMonth;

    @Column(name = "is_weekly_checkpoint_day", nullable = false)
    private Boolean isWeeklyCheckpointDay = false;

    @Column(name = "streak_count", nullable = false)
    private Integer streakCount = 0;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({"users"})
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User employee;

    @OneToMany(mappedBy = "dayWorkspace", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"dayWorkspace"})
    private List<Task> tasks;

    public DayWorkspace() {}
}
