package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_comments")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"comments", "taskComments"})
    private Task task;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commented_by", nullable = false)
    @JsonIgnoreProperties({"comments", "taskComments"})
    private User commentedBy;

    public TaskComment() {}

    public TaskComment(Task task, String comment, User commentedBy) {
        this.task = task;
        this.comment = comment;
        this.commentedBy = commentedBy;
    }
}
