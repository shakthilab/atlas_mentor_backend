package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "time_tracking",
       indexes = {
           @Index(name = "idx_time_tracking_task_id", columnList = "task_id"),
           @Index(name = "idx_time_tracking_user_id", columnList = "user_id"),
           @Index(name = "idx_time_tracking_start_time", columnList = "start_time")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TimeTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_in_seconds")
    private Long durationInSeconds;

    public TimeTracking() {}

    public TimeTracking(Task task, User user, LocalDateTime startTime) {
        this.task = task;
        this.user = user;
        this.startTime = startTime;
    }
}
