package br.com.vdb.core.domain.model;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff with jitter: initialBackoff * 2^(attempts - 1), limited to maxBackoff,
 * then changed by a random +/- {@code jitter} percentage so failed requests do not all retry at the same time.
 *
 * @param maxAttempts total number of calls to the provider, including the first one
 * @param jitter      between 0 (no jitter) and 1; 0.2 means +/- 20%
 */
public record RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff, double jitter) {

    private static final int MAX_EXPONENT = 20;

    public RetryPolicy {
        Objects.requireNonNull(initialBackoff, "initialBackoff must not be null");
        Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (jitter < 0 || jitter > 1) {
            throw new IllegalArgumentException("jitter must be between 0 and 1");
        }
    }

    public boolean canRetry(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    /**
     * @param attemptsMade attempts already made (1 after the first call)
     * @param retryAfter   wait time asked by the provider (Retry-After header), or null
     */
    public Duration nextDelay(int attemptsMade, Duration retryAfter) {
        int exponent = Math.min(Math.max(attemptsMade - 1, 0), MAX_EXPONENT);
        long backoff = Math.min(initialBackoff.toMillis() << exponent, maxBackoff.toMillis());
        double randomFactor = 1 + jitter * (ThreadLocalRandom.current().nextDouble() * 2 - 1);
        long delay = Math.round(backoff * randomFactor);

        if (retryAfter != null) {
            // Respect the provider, but never wait longer than maxBackoff.
            delay = Math.max(delay, Math.min(retryAfter.toMillis(), maxBackoff.toMillis()));
        }
        return Duration.ofMillis(delay);
    }
}
