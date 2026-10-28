package br.com.vdb.core.domain.model;

import br.com.vdb.core.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationRequestTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(5);

    private final ActivationRequest pending =
            ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "client-a", "corr-1", NOW);

    @Test
    void newRequestIsPendingAndReadyToSend() {
        assertThat(pending.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(pending.attempts()).isZero();
        assertThat(pending.nextAttemptAt()).isEqualTo(NOW);
        assertThat(pending.createdAt()).isEqualTo(NOW);
        assertThat(pending.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void startProcessingCountsOneAttempt() {
        ActivationRequest processing = pending.startProcessing(LATER);

        assertThat(processing.status()).isEqualTo(ActivationStatus.PROCESSING);
        assertThat(processing.attempts()).isEqualTo(1);
        assertThat(processing.nextAttemptAt()).isNull();
        assertThat(processing.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void completesFromProcessing() {
        ActivationRequest completed = pending.startProcessing(NOW).complete(LATER);

        assertThat(completed.status()).isEqualTo(ActivationStatus.COMPLETED);
        assertThat(completed.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void failsFromProcessingKeepingTheReason() {
        ActivationRequest failed = pending.startProcessing(NOW).fail("rejected", LATER);

        assertThat(failed.status()).isEqualTo(ActivationStatus.FAILED);
        assertThat(failed.lastError()).isEqualTo("rejected");
    }

    @Test
    void retryLaterGoesBackToPendingWithNextAttemptTime() {
        Instant next = LATER.plusSeconds(10);

        ActivationRequest retry = pending.startProcessing(NOW).retryLater("timeout", next, LATER);

        assertThat(retry.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(retry.attempts()).isEqualTo(1);
        assertThat(retry.nextAttemptAt()).isEqualTo(next);
        assertThat(retry.lastError()).isEqualTo("timeout");
    }

    @Test
    void pendingCannotBeCompletedDirectly() {
        assertThatThrownBy(() -> pending.complete(LATER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void completedCannotGoBackToProcessing() {
        ActivationRequest completed = pending.startProcessing(NOW).complete(LATER);

        assertThatThrownBy(() -> completed.startProcessing(LATER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void failedIsFinal() {
        ActivationRequest failed = pending.startProcessing(NOW).fail("rejected", LATER);

        assertThatThrownBy(() -> failed.retryLater("again", LATER, LATER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void sameRequestComparesClientCustomerAndProduct() {
        assertThat(pending.isSameRequest("client-a", "CUST-1", "INTERNET-500")).isTrue();
        assertThat(pending.isSameRequest("client-a", "CUST-1", "TV-HD")).isFalse();
        assertThat(pending.isSameRequest("client-b", "CUST-1", "INTERNET-500")).isFalse();
    }
}
