package com.ras.safetyform.service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limits failed login attempts per client address and username, and per client
 * address overall. State is in memory, so limits apply per application instance.
 */
@Component
public class LoginRateLimiter {

    private static final int CLIENT_LIMIT_MULTIPLIER = 4;
    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(
            @Value("${app.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.login-rate-limit.window:15m}") Duration window) {
        this(maxAttempts, window, Clock.systemUTC());
    }

    LoginRateLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Throws when the client has used up its failed attempts for the current window. */
    public void assertAllowed(String clientAddress, String username) {
        long now = clock.millis();
        long retryAfter = Math.max(
                retryAfterMillis(accountKey(clientAddress, username), maxAttempts, now),
                retryAfterMillis(clientKey(clientAddress), maxAttempts * CLIENT_LIMIT_MULTIPLIER, now));
        if (retryAfter > 0) {
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Try again later.",
                    Math.max(1, (retryAfter + 999) / 1000));
        }
    }

    public void recordFailure(String clientAddress, String username) {
        long now = clock.millis();
        if (failures.size() > PURGE_THRESHOLD) {
            purgeExpired(now);
        }
        record(accountKey(clientAddress, username), now);
        record(clientKey(clientAddress), now);
    }

    public void recordSuccess(String clientAddress, String username) {
        failures.remove(accountKey(clientAddress, username));
    }

    private void record(String key, long now) {
        Deque<Long> attempts = failures.computeIfAbsent(key, (ignored) -> new ArrayDeque<>());
        synchronized (attempts) {
            dropExpired(attempts, now);
            attempts.addLast(now);
        }
    }

    private long retryAfterMillis(String key, int limit, long now) {
        Deque<Long> attempts = failures.get(key);
        if (attempts == null) {
            return 0;
        }
        synchronized (attempts) {
            dropExpired(attempts, now);
            if (attempts.size() < limit) {
                return 0;
            }
            // The oldest counted failure leaves the window first, freeing one attempt.
            long oldestCounted = attempts.toArray(new Long[0])[attempts.size() - limit];
            return oldestCounted + window.toMillis() - now;
        }
    }

    private void dropExpired(Deque<Long> attempts, long now) {
        long cutoff = now - window.toMillis();
        while (!attempts.isEmpty() && attempts.peekFirst() <= cutoff) {
            attempts.removeFirst();
        }
    }

    private void purgeExpired(long now) {
        failures.entrySet().removeIf((entry) -> {
            synchronized (entry.getValue()) {
                dropExpired(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private String accountKey(String clientAddress, String username) {
        return "account|" + clientAddress + "|" + username.strip().toLowerCase(Locale.ROOT);
    }

    private String clientKey(String clientAddress) {
        return "client|" + clientAddress;
    }
}
