package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.RecurringFrequency;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "recurring_tasks",
       indexes = {
           @Index(name = "idx_recurring_tasks_task_id", columnList = "task_id"),
           @Index(name = "idx_recurring_tasks_next_execution", columnList = "next_execution_time"),
           @Index(name = "idx_recurring_tasks_frequency", columnList = "frequency")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecurringTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurringFrequency frequency;

    @Column(name = "interval_value")
    private Integer intervalValue = 1;

    @Column(name = "next_execution_time")
    private LocalDateTime nextExecutionTime;

    public RecurringTask() {}

    public RecurringTask(Task task, RecurringFrequency frequency, Integer intervalValue, LocalDateTime nextExecutionTime) {
        this.task = task;
        this.frequency = frequency;
        this.intervalValue = intervalValue != null ? intervalValue : 1;
        this.nextExecutionTime = nextExecutionTime;
    }
}
