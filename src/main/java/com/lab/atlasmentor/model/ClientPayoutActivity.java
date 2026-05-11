package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.ClientPayoutAction;
import com.lab.atlasmentor.enums.DisputeStage;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_payout_activities",
       indexes = {
           @Index(name = "idx_client_payout_activities_payout_id", columnList = "client_payout_id"),
           @Index(name = "idx_client_payout_activities_action", columnList = "action"),
           @Index(name = "idx_client_payout_activities_done_at", columnList = "done_at")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClientPayoutActivity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_payout_id", nullable = false)
    @JsonIgnoreProperties({"activities"})
    private ClientPayout clientPayout;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ClientPayoutAction action;
    
    @Column(name = "old_value", length = 500)
    private String oldValue;
    
    @Column(name = "new_value", length = 500)
    private String newValue;
    
    @Column(name = "old_amount", precision = 10, scale = 2)
    private BigDecimal oldAmount;
    
    @Column(name = "new_amount", precision = 10, scale = 2)
    private BigDecimal newAmount;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by", nullable = false)
    @JsonIgnoreProperties({"clientPayouts", "activities"})
    private User doneBy;
    
    @Column(name = "done_at", nullable = false)
    private LocalDateTime doneAt;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_stage")
    private DisputeStage disputeStage;
    
    @Column(name = "previous_status", length = 50)
    private String previousStatus;
    
    @Column(name = "new_status", length = 50)
    private String newStatus;
    
    public ClientPayoutActivity() {}
    
    public ClientPayoutActivity(ClientPayout clientPayout, ClientPayoutAction action,
                               String oldValue, String newValue, String reason, User doneBy) {
        this.clientPayout = clientPayout;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.doneBy = doneBy;
        this.doneAt = LocalDateTime.now();
    }
    
    public ClientPayoutActivity(ClientPayout clientPayout, ClientPayoutAction action,
                               BigDecimal oldAmount, BigDecimal newAmount, String reason, User doneBy) {
        this.clientPayout = clientPayout;
        this.action = action;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.reason = reason;
        this.doneBy = doneBy;
        this.doneAt = LocalDateTime.now();
    }
}
