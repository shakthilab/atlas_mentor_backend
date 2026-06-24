package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "notifications",
       indexes = {
           @Index(name = "idx_notifications_user_id", columnList = "user_id"),
           @Index(name = "idx_notifications_is_read", columnList = "is_read"),
           @Index(name = "idx_notifications_type", columnList = "type"),
           @Index(name = "idx_notifications_created_at", columnList = "created_at")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public Notification() {}

    public Notification(User user, NotificationType type, Long referenceId, String title, String message) {
        this.user = user;
        this.type = type;
        this.referenceId = referenceId;
        this.title = title;
        this.message = message;
        this.isRead = false;
    }
}
