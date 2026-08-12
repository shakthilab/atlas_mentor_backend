package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_approvals",
       indexes = {
           @Index(name = "idx_day_approvals_workspace", columnList = "day_workspace_id"),
           @Index(name = "idx_day_approvals_approver", columnList = "approver_user_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DayApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "stage", nullable = false)
    private String stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_workspace_id", nullable = false)
    @JsonIgnoreProperties({"tasks"})
    private DayWorkspace dayWorkspace;

    public DayApproval() {}
}
