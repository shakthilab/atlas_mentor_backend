package com.lab.atlasmentor.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window per-IP rate limiter for auth endpoints.
 * 10 requests per 15 minutes; state is in-memory (resets on restart).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, ArrayDeque<Long>> windows = new ConcurrentHashMap<>();

    /**
     * Records the request and returns true if it is within the allowed rate.
     */
    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> window = windows.compute(ip, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) deque.pollFirst();
            deque.addLast(now);
            return deque;
        });
        return window.size() <= MAX_REQUESTS;
    }

    /** Seconds until the oldest request in the window falls outside the window. */
    public long retryAfterSeconds(String ip) {
        ArrayDeque<Long> window = windows.get(ip);
        if (window == null || window.isEmpty()) return 0;
        return Math.max(0, (window.peekFirst() + WINDOW_MS - System.currentTimeMillis()) / 1000);
    }
}
