package br.com.vdb.core.port.in;

public record ActivateProductCommand(String externalId, String customerId, String productCode) {
}
