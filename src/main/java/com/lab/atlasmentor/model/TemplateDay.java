package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Entity
@Table(name = "template_days",
       indexes = {
           @Index(name = "idx_template_days_role_template", columnList = "role_template_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemplateDay extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "is_weekly_checkpoint", nullable = false)
    private Boolean isWeeklyCheckpoint = false;

    // Null month/year = recurring day, applies to this dayNumber every month (legacy behavior).
    // Both set = scoped day, applies only to that specific calendar month.
    @Column(name = "month")
    private Integer month;

    @Column(name = "year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_template_id", nullable = false)
    @JsonIgnoreProperties({"executions", "tasks", "taskLists"})
    private TaskBundle roleTemplate;

    @OneToMany(mappedBy = "templateDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"templateDay"})
    private List<TemplateTask> templateTasks;

    public TemplateDay() {}
}
