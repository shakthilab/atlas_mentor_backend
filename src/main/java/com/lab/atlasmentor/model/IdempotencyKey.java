package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One row per client-supplied {@code Idempotency-Key} seen on a mutating, unsafe-to-replay
 * request (day duplication, bulk task cloning). See {@link com.lab.atlasmentor.service.IdempotencyService}
 * for how a row moves from "claimed" (response fields null) to "completed" (response fields
 * set, safe to replay verbatim to any retry of the same key).
 */
@Entity
@Table(name = "idempotency_keys",
       indexes = {
           @Index(name = "idx_idempotency_keys_created_at", columnList = "created_at")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IdempotencyKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    // Which operation this key was claimed for - purely informational (debugging/auditing),
    // not part of the uniqueness or replay-matching logic.
    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    // SHA-256 of the request payload. Lets a reused key be told apart from a genuinely
    // different request accidentally reusing the same key (client bug) instead of silently
    // replaying the wrong result.
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    // Null while the original request is still executing - see IdempotencyService.begin().
    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public IdempotencyKey() {}
}
