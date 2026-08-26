-- Backs IdempotencyService: lets a client-supplied Idempotency-Key header on a mutating,
-- unsafe-to-replay request (day duplication, bulk task cloning) be recognized on retry so
-- the server returns the original result instead of running the mutation again. Without
-- this, a network retry or a resubmitted request duplicates every task it touches - the
-- exact "10 tasks became 20" failure shape, just triggered by a repeated request instead
-- of a self-targeted one.
CREATE TABLE IF NOT EXISTS idempotency_keys (
                                                id BIGSERIAL PRIMARY KEY,
                                                idempotency_key VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    -- Both null while the original request is still being processed; a concurrent replay
    -- arriving in that window is told to wait rather than shown a stale/empty result.
    response_status INTEGER,
    response_body TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    completed_at TIMESTAMP
    );

-- This is what actually makes claiming a key race-safe under concurrent duplicate
-- submissions: two requests racing to INSERT the same key can't both succeed, so the loser
-- always sees a row it can replay from or wait on - see IdempotencyService.begin().
CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_key ON idempotency_keys (idempotency_key);

-- Supports the scheduled cleanup in IdempotencyService (purges keys older than 48h) without
-- a full table scan.
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at ON idempotency_keys (created_at);