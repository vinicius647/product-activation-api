package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ActivationStatus;

/**
 * Filters for listing requests. {@code status} and {@code customerId} are optional (null = any).
 * Results are always limited to the requests of {@code clientId}.
 */
public record ActivationSearch(
        String clientId,
        ActivationStatus status,
        String customerId,
        int page,
        int size
) {
}
