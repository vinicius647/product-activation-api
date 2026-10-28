package br.com.vdb.core.port.in;

public record CreateActivationCommand(
        String externalId,
        String customerId,
        String productCode,
        String clientId,
        String correlationId
) {
}
