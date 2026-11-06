package com.admin.template.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterContractTest {
    @Test
    void startsANewFixedWindowAfterTheFirstAttemptExpires() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        Instant start = Instant.parse("2026-10-22T04:00:00Z");
        assertThat(limiter.tryAcquire("203.0.113.20", start)).isTrue();
        for (int attempt = 1; attempt < 10; attempt++) {
            assertThat(limiter.tryAcquire("203.0.113.20", start.plusSeconds(30))).isTrue();
        }
        assertThat(limiter.tryAcquire("203.0.113.20", start.plusSeconds(59))).isFalse();

        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(limiter.tryAcquire("203.0.113.20", start.plusSeconds(60))).isTrue();
        }
    }

    @Test
    void roundsRetryAfterUpToTheRemainingWholeSecond() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        Instant start = Instant.parse("2026-10-22T04:00:00Z");
        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(limiter.tryAcquire("203.0.113.21", start)).isTrue();
        }

        assertThat(limiter.retryAfterSeconds("203.0.113.21", start.plusMillis(100))).isEqualTo(60);
    }
}
