package br.com.vdb.core.domain.event;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Sent when a request reaches a final status (COMPLETED or FAILED), so other systems can react.
 *
 * @param reason why the request failed; null when it was completed
 */
public record ActivationFinishedEvent(
        UUID requestId,
        String externalId,
        String customerId,
        String productCode,
        String clientId,
        ActivationStatus status,
        String reason,
        Instant finishedAt
) {

    public static ActivationFinishedEvent from(ActivationRequest request) {
        if (!request.status().isFinal()) {
            throw new IllegalArgumentException("Request " + request.id() + " is not finished: " + request.status());
        }
        String reason = request.status() == ActivationStatus.FAILED ? request.lastError() : null;
        return new ActivationFinishedEvent(request.id(), request.externalId(), request.customerId(),
                request.productCode(), request.clientId(), request.status(), reason, request.updatedAt());
    }
}
