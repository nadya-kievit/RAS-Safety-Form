package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(15), clock);

    @Test
    void blocksAfterTheAllowedNumberOfFailures() {
        for (int attempt = 0; attempt < 3; attempt++) {
            assertDoesNotThrow(() -> limiter.assertAllowed("10.0.0.1", "alex"));
            limiter.recordFailure("10.0.0.1", "alex");
        }

        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> limiter.assertAllowed("10.0.0.1", "alex"));
        assertTrue(exception.getRetryAfterSeconds() > 0);
        assertTrue(exception.getRetryAfterSeconds() <= 15 * 60);
    }

    @Test
    void usernameMatchingIgnoresCaseAndSurroundingWhitespace() {
        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.recordFailure("10.0.0.1", attempt == 0 ? "Alex" : " alex ");
        }

        assertThrows(
                TooManyRequestsException.class,
                () -> limiter.assertAllowed("10.0.0.1", "ALEX"));
    }

    @Test
    void failuresForOneUserDoNotBlockAnotherUserFromTheSameAddress() {
        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.recordFailure("10.0.0.1", "alex");
        }

        assertDoesNotThrow(() -> limiter.assertAllowed("10.0.0.1", "sam"));
        assertDoesNotThrow(() -> limiter.assertAllowed("10.0.0.2", "alex"));
    }

    @Test
    void anAddressCannotGuessAtManyUsernames() {
        for (int attempt = 0; attempt < 12; attempt++) {
            limiter.recordFailure("10.0.0.1", "user-" + attempt);
        }

        assertThrows(
                TooManyRequestsException.class,
                () -> limiter.assertAllowed("10.0.0.1", "brand-new-user"));
    }

    @Test
    void allowsAttemptsAgainOnceTheWindowHasPassed() {
        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.recordFailure("10.0.0.1", "alex");
        }

        clock.advance(Duration.ofMinutes(16));

        assertDoesNotThrow(() -> limiter.assertAllowed("10.0.0.1", "alex"));
    }

    @Test
    void successfulLoginClearsTheAccountFailures() {
        limiter.recordFailure("10.0.0.1", "alex");
        limiter.recordFailure("10.0.0.1", "alex");

        limiter.recordSuccess("10.0.0.1", "alex");
        limiter.recordFailure("10.0.0.1", "alex");

        assertDoesNotThrow(() -> limiter.assertAllowed("10.0.0.1", "alex"));
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-07T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
