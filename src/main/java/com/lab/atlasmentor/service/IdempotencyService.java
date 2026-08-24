package com.lab.atlasmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lab.atlasmentor.exception.ConflictException;
import com.lab.atlasmentor.model.IdempotencyKey;
import com.lab.atlasmentor.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Guards a mutating, unsafe-to-replay endpoint (day duplication, bulk task cloning) against
 * being executed twice for what is really one logical request - a network retry, a client
 * resubmitting after what looked like a hang, or (rarer, but the reason this uses a real DB
 * unique constraint rather than a simple "have I seen this key" check) two truly concurrent
 * copies of the same request landing at once. Without this, a repeated request duplicates
 * every task it touches - the same failure shape as duplicating a day onto itself, just
 * triggered by a repeated request instead of a self-targeted one.
 *
 * Usage in a controller:
 * <pre>
 *   Outcome outcome = idempotencyService.begin(key, "someEndpoint", requestPayload, userId);
 *   if (outcome instanceof Replay replay) {
 *       return idempotencyService.replayAs(replay, new TypeReference&lt;ApiResponse&lt;X&gt;&gt;() {});
 *   }
 *   // ... run the real mutation ...
 *   idempotencyService.complete(((Proceed) outcome).rowId(), 200, responseBody);
 * </pre>
 * The {@code Idempotency-Key} header is optional on every endpoint that accepts it - a
 * caller that doesn't send one gets the exact old behavior, unprotected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    // Deliberately a dedicated instance rather than the app's shared ObjectMapper bean
    // (see WebConfig) - this only ever serializes our own request/response payloads for
    // internal bookkeeping, so it's simpler to own a correctly-configured mapper outright
    // than to depend on the shared bean's configuration never changing under it.
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public sealed interface Outcome permits Proceed, Replay {}

    /** The caller claimed the key and must now run the real mutation, then call {@link #complete}. */
    public record Proceed(Long rowId) implements Outcome {}

    /** A prior (or concurrently-completed) request already ran this exact request; replay its result verbatim. */
    public record Replay(int status, String responseBodyJson) implements Outcome {}

    /**
     * Claims {@code idempotencyKey} for {@code endpoint}, or returns the result to replay if
     * it was already (or is currently being) processed.
     *
     * Runs in its own committed transaction (REQUIRES_NEW) rather than inside the caller's
     * business transaction: the claim row must become visible to a concurrent duplicate
     * request immediately, not only after the whole mutation finishes - that's what closes
     * the race two simultaneous copies of the same request would otherwise hit.
     *
     * @throws ConflictException if the key was already used for a request with different
     *         content (a client bug - reusing a key across two different actions), or if the
     *         original request is still in flight (ask the caller to wait, not resubmit).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome begin(String idempotencyKey, String endpoint, Object requestPayload, Long userId) {
        String hash = hash(requestPayload);

        Optional<IdempotencyKey> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), hash);
        }

        IdempotencyKey row = new IdempotencyKey();
        row.setIdempotencyKey(idempotencyKey);
        row.setEndpoint(endpoint);
        row.setRequestHash(hash);
        row.setCreatedBy(userId);
        try {
            repository.saveAndFlush(row);
            return new Proceed(row.getId());
        } catch (DataIntegrityViolationException race) {
            // Lost the race to claim this key - another request (very likely a true
            // duplicate of this same one) claimed it in the gap between our lookup above and
            // this insert. Fall back to whatever that request left behind, exactly as if
            // we'd found it on the first lookup.
            IdempotencyKey winner = repository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> race);
            return resolveExisting(winner, hash);
        }
    }

    private Outcome resolveExisting(IdempotencyKey row, String hash) {
        if (!row.getRequestHash().equals(hash)) {
            throw new ConflictException(
                    "Idempotency-Key '" + row.getIdempotencyKey() + "' was already used for a different request.");
        }
        if (row.getResponseStatus() == null) {
            throw new ConflictException(
                    "This request is still being processed - please wait a moment rather than resubmitting.");
        }
        return new Replay(row.getResponseStatus(), row.getResponseBody());
    }

    /**
     * Releases a claim after the guarded operation threw instead of completing - nothing
     * was persisted, so there's no result to replay, and leaving the claim behind would
     * permanently stick any retry with this key at "still being processed" (see
     * {@link #resolveExisting}) until the 48h cleanup sweep happens to purge it. Deleting it
     * lets a corrected retry with the same key go through {@link #begin} again as if the key
     * had never been used.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseOnFailure(Long rowId) {
        repository.deleteById(rowId);
    }

    /**
     * Records the outcome of the mutation the caller was cleared to run via {@link #begin},
     * so any later replay of the same key returns this result instead of running it again.
     * Own transaction for the same reason as {@code begin} - this must be durable independent
     * of whatever the caller's own transaction does next.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long rowId, int status, Object responseBody) {
        IdempotencyKey row = repository.findById(rowId).orElse(null);
        if (row == null) {
            // Bookkeeping row vanished somehow - log and move on rather than failing the
            // response the caller is about to return; the real work already succeeded.
            log.warn("Idempotency key row {} not found when recording completion", rowId);
            return;
        }
        row.setResponseStatus(status);
        row.setResponseBody(toJson(responseBody));
        row.setCompletedAt(LocalDateTime.now());
        repository.save(row);
    }

    private String hash(Object payload) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to hash idempotency request payload", e);
        }
    }

    String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotent response for replay", e);
        }
    }

    ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Deserializes a {@link Replay}'s stored response back into the same shape the original
     * request returned, so the controller can hand it back to the client exactly as-is.
     */
    public <T> T replayAs(Replay replay, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        try {
            return MAPPER.readValue(replay.responseBodyJson(), type);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotent response", e);
        }
    }

    /**
     * Purges keys old enough that nothing will ever legitimately retry with them, so this
     * table doesn't grow unbounded. 48h is generous relative to how long a client would ever
     * plausibly hold onto a key to retry a "hung" request.
     */
    @Scheduled(fixedRate = 21_600_000) // every 6 hours
    @Transactional
    public void cleanupOldKeys() {
        int deleted = repository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(48));
        if (deleted > 0) {
            log.info("Idempotency key cleanup: purged {} key(s) older than 48h", deleted);
        }
    }
}
