package com.admin.template.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public boolean tryAcquire(String ip) {
        return tryAcquire(ip, Instant.now());
    }

    boolean tryAcquire(String ip, Instant now) {
        if (attemptsByIp.size() > 1000) {
            attemptsByIp.entrySet().removeIf(entry -> {
                Deque<Instant> attempts = entry.getValue();
                return attempts.isEmpty() || !now.isBefore(attempts.peekFirst().plus(WINDOW));
            });
        }
        String key = ip == null || ip.isBlank() ? "unknown" : ip;
        boolean[] acquired = {false};
        attemptsByIp.compute(key, (ignored, previous) -> {
            Deque<Instant> attempts = previous == null ? new ArrayDeque<>() : previous;
            if (!attempts.isEmpty() && !now.isBefore(attempts.peekFirst().plus(WINDOW))) {
                attempts.clear();
            }
            if (attempts.size() < MAX_ATTEMPTS) {
                attempts.addLast(now);
                acquired[0] = true;
            }
            return attempts;
        });
        return acquired[0];
    }

    public long retryAfterSeconds(String ip, Instant now) {
        String key = ip == null || ip.isBlank() ? "unknown" : ip;
        long[] retryAfter = {0};
        attemptsByIp.computeIfPresent(key, (ignored, attempts) -> {
            if (!attempts.isEmpty()) {
                long remainingMillis = Duration.between(now, attempts.peekFirst().plus(WINDOW)).toMillis();
                if (remainingMillis > 0) {
                    retryAfter[0] = Math.max(1, (remainingMillis + 999) / 1000);
                } else {
                    attempts.clear();
                }
            }
            return attempts.isEmpty() ? null : attempts;
        });
        return retryAfter[0];
    }
}
