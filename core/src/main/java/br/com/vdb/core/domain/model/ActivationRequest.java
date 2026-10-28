package br.com.vdb.core.domain.model;

import br.com.vdb.core.domain.exception.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: a request from a client system to activate a product for a customer.
 * <p>
 * Immutable. Every status change goes through a method that checks the allowed transitions
 * and returns a new instance.
 *
 * @param clientId      system that created the request (taken from the access token)
 * @param attempts      number of calls already sent to the provider
 * @param nextAttemptAt when the request can be sent to the provider; null when the status is final
 * @param correlationId id of the HTTP request that created it, used in the logs of the async processing
 * @param version       optimistic lock version; null before the first save
 */
public record ActivationRequest(
        UUID id,
        String externalId,
        String customerId,
        String productCode,
        String clientId,
        ActivationStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        String correlationId,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {

    public ActivationRequest {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        requireNotBlank(externalId, "externalId");
        requireNotBlank(customerId, "customerId");
        requireNotBlank(productCode, "productCode");
        requireNotBlank(clientId, "clientId");
    }

    public static ActivationRequest create(String externalId, String customerId, String productCode,
                                           String clientId, String correlationId, Instant now) {
        return new ActivationRequest(UUID.randomUUID(), externalId, customerId, productCode, clientId,
                ActivationStatus.PENDING, 0, now, null, correlationId, null, now, now);
    }

    /**
     * True when a repeated request (same externalId) carries the same data, i.e. it is a legitimate retry.
     */
    public boolean isSameRequest(String clientId, String customerId, String productCode) {
        return this.clientId.equals(clientId)
                && this.customerId.equals(customerId)
                && this.productCode.equals(productCode);
    }

    /** Claims the request to be sent to the provider. Each claim counts as one attempt. */
    public ActivationRequest startProcessing(Instant now) {
        checkTransition(ActivationStatus.PROCESSING);
        return with(ActivationStatus.PROCESSING, attempts + 1, null, lastError, now);
    }

    public ActivationRequest complete(Instant now) {
        checkTransition(ActivationStatus.COMPLETED);
        return with(ActivationStatus.COMPLETED, attempts, null, null, now);
    }

    public ActivationRequest fail(String reason, Instant now) {
        checkTransition(ActivationStatus.FAILED);
        return with(ActivationStatus.FAILED, attempts, null, reason, now);
    }

    /** Temporary failure: goes back to PENDING and waits until {@code nextAttemptAt}. */
    public ActivationRequest retryLater(String reason, Instant nextAttemptAt, Instant now) {
        checkTransition(ActivationStatus.PENDING);
        return with(ActivationStatus.PENDING, attempts, nextAttemptAt, reason, now);
    }

    private void checkTransition(ActivationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
    }

    private ActivationRequest with(ActivationStatus newStatus, int newAttempts, Instant newNextAttemptAt,
                                   String newLastError, Instant now) {
        return new ActivationRequest(id, externalId, customerId, productCode, clientId, newStatus, newAttempts,
                newNextAttemptAt, newLastError, correlationId, version, createdAt, now);
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
