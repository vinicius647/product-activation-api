package br.com.vdb.controller.dto;

import br.com.vdb.core.port.in.ActivateProductCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to activate a product for a customer")
public record ActivationRequest(
        @Schema(example = "EXT-12345") @NotBlank @Size(max = 100) String externalId,
        @Schema(example = "CUST-10001") @NotBlank @Size(max = 100) String customerId,
        @Schema(example = "INTERNET-500") @NotBlank @Size(max = 100) String productCode
) {

    public ActivateProductCommand toCommand() {
        return new ActivateProductCommand(externalId, customerId, productCode);
    }
}
