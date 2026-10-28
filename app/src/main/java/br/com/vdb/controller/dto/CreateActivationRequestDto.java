package br.com.vdb.controller.dto;

import br.com.vdb.core.port.in.CreateActivationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to activate a product for a customer")
public record CreateActivationRequestDto(
        @Schema(description = "Id of the request in the client system. Must be unique.", example = "EXT-12345",
                maxLength = 64, pattern = CODE_PATTERN)
        @NotBlank @Size(max = 64) @Pattern(regexp = CODE_PATTERN, message = CODE_MESSAGE)
        String externalId,

        @Schema(example = "CUST-10001", maxLength = 64, pattern = CODE_PATTERN)
        @NotBlank @Size(max = 64) @Pattern(regexp = CODE_PATTERN, message = CODE_MESSAGE)
        String customerId,

        @Schema(example = "INTERNET-500", maxLength = 64, pattern = CODE_PATTERN)
        @NotBlank @Size(max = 64) @Pattern(regexp = CODE_PATTERN, message = CODE_MESSAGE)
        String productCode
) {

    static final String CODE_PATTERN = "^[A-Za-z0-9._-]*$";
    static final String CODE_MESSAGE = "must contain only letters, numbers, '.', '_' or '-'";

    public CreateActivationCommand toCommand(String clientId, String correlationId) {
        return new CreateActivationCommand(externalId, customerId, productCode, clientId, correlationId);
    }
}
