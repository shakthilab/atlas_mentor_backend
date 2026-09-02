package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.Priority;

@Entity
@Table(name = "template_tasks",
       indexes = {
           @Index(name = "idx_template_tasks_day", columnList = "template_day_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemplateTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_day_id", nullable = false)
    @JsonIgnoreProperties({"templateTasks", "roleTemplate"})
    private TemplateDay templateDay;

    /**
     * Whether an employee must attach at least one proof-section file (comment_id IS
     * NULL on task_attachments) before a task instantiated from this template task can
     * be marked DONE - see TaskService#updateTaskStatus. Copied as a one-time snapshot
     * onto Task.proofRequired at instantiation (TemplateInstantiationService); editing
     * this afterwards never retroactively affects already-assigned tasks.
     */
    @Column(name = "proof_required", nullable = false)
    private Boolean proofRequired = false;

    public TemplateTask() {}
}
