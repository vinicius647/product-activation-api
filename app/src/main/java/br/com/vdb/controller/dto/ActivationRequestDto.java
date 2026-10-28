package br.com.vdb.controller.dto;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivationRequestDto(
        UUID id,
        String externalId,
        String customerId,
        String productCode,
        ActivationStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ActivationRequestDto from(ActivationRequest request) {
        return new ActivationRequestDto(
                request.id(),
                request.externalId(),
                request.customerId(),
                request.productCode(),
                request.status(),
                request.createdAt(),
                request.updatedAt());
    }
}
