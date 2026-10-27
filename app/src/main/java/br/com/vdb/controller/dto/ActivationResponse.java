package br.com.vdb.controller.dto;

import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.domain.model.ProductActivation;

import java.time.Instant;
import java.util.UUID;

public record ActivationResponse(
        UUID id,
        String externalId,
        String customerId,
        String productCode,
        ActivationStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ActivationResponse from(ProductActivation activation) {
        return new ActivationResponse(
                activation.id(),
                activation.externalId(),
                activation.customerId(),
                activation.productCode(),
                activation.status(),
                activation.createdAt(),
                activation.updatedAt());
    }
}
