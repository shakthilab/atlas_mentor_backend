package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.PaymentDisputeAction;
import com.lab.atlasmentor.enums.DisputeStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_dispute_activity",
       uniqueConstraints = @UniqueConstraint(columnNames = "payment_id", name = "uk_payment_dispute"))
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentDisputeActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    @JsonIgnoreProperties({"activities", "paymentDisputeActivities"})
    private StudentPayment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private PaymentDisputeAction action;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status = DisputeStatus.INITIATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by", nullable = false)
    @JsonIgnoreProperties({"activities", "paymentDisputeActivities"})
    private User doneBy;

    @Column(name = "done_at", nullable = false)
    private LocalDateTime doneAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PaymentDisputeActivity() {}

    public PaymentDisputeActivity(StudentPayment payment, PaymentDisputeAction action,
                                 String oldValue, String newValue, String reason, User doneBy) {
        this.payment = payment;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.doneBy = doneBy;
        this.doneAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = DisputeStatus.INITIATED;
    }
}
