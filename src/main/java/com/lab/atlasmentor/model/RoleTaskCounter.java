package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "role_task_counters")
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleTaskCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "last_sequence_number", nullable = false)
    private Long lastSequenceNumber = 0L;

    public RoleTaskCounter() {}

    public RoleTaskCounter(Role role, Long lastSequenceNumber) {
        this.role = role;
        this.lastSequenceNumber = lastSequenceNumber;
    }
}
