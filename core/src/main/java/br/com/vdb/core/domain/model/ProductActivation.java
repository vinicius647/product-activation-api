package br.com.vdb.core.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a request to activate a product for a customer.
 */
public record ProductActivation(
        UUID id,
        String externalId,
        String customerId,
        String productCode,
        ActivationStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public ProductActivation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        requireNotBlank(externalId, "externalId");
        requireNotBlank(customerId, "customerId");
        requireNotBlank(productCode, "productCode");
    }

    public static ProductActivation create(String externalId, String customerId, String productCode, Instant now) {
        return new ProductActivation(UUID.randomUUID(), externalId, customerId, productCode,
                ActivationStatus.PENDING, now, now);
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
