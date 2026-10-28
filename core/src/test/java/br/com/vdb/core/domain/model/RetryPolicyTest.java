package br.com.vdb.core.domain.model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy noJitter = new RetryPolicy(5, Duration.ofSeconds(2), Duration.ofMinutes(1), 0);

    @Test
    void doublesTheDelayOnEachAttempt() {
        assertThat(noJitter.nextDelay(1, null)).isEqualTo(Duration.ofSeconds(2));
        assertThat(noJitter.nextDelay(2, null)).isEqualTo(Duration.ofSeconds(4));
        assertThat(noJitter.nextDelay(3, null)).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void neverWaitsMoreThanMaxBackoff() {
        assertThat(noJitter.nextDelay(30, null)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void usesRetryAfterWhenItIsLongerThanTheBackoff() {
        assertThat(noJitter.nextDelay(1, Duration.ofSeconds(30))).isEqualTo(Duration.ofSeconds(30));
        assertThat(noJitter.nextDelay(1, Duration.ofHours(1))).isEqualTo(Duration.ofMinutes(1));
    }

    @RepeatedTest(20)
    void jitterKeepsTheDelayInsideTheRange() {
        var policy = new RetryPolicy(5, Duration.ofSeconds(10), Duration.ofMinutes(1), 0.2);

        assertThat(policy.nextDelay(1, null)).isBetween(Duration.ofSeconds(8), Duration.ofSeconds(12));
    }

    @Test
    void allowsRetryUntilMaxAttempts() {
        assertThat(noJitter.canRetry(4)).isTrue();
        assertThat(noJitter.canRetry(5)).isFalse();
    }
}
