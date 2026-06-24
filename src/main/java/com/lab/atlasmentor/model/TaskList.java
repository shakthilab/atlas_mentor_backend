package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_lists",
       indexes = {
           @Index(name = "idx_task_lists_bundle_id", columnList = "task_bundle_id"),
           @Index(name = "idx_task_lists_display_order", columnList = "display_order")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_bundle_id", nullable = false)
    @JsonIgnoreProperties({"taskLists", "tasks", "schedule", "executions"})
    private TaskBundle taskBundle;

    @OneToMany(mappedBy = "taskList", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"taskList"})
    private List<Task> tasks;

    public TaskList() {}

    public TaskList(String name, String description, String color, Integer displayOrder, TaskBundle taskBundle) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.taskBundle = taskBundle;
    }
}
