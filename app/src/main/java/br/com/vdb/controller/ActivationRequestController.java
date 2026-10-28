package br.com.vdb.controller;

import br.com.vdb.config.CorrelationIdFilter;
import br.com.vdb.controller.dto.ActivationRequestDto;
import br.com.vdb.controller.dto.CreateActivationRequestDto;
import br.com.vdb.controller.dto.PageDto;
import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.port.in.ActivationSearch;
import br.com.vdb.core.port.in.CreateActivationResult;
import br.com.vdb.core.port.in.CreateActivationUseCase;
import br.com.vdb.core.port.in.FindActivationUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * All endpoints share the "requestsApi" rate limiter. The caller system (client id) comes from the token.
 */
@RestController
@RequestMapping("/api/v1/requests")
@RateLimiter(name = ActivationRequestController.RATE_LIMITER)
@Tag(name = "Activation requests")
@SecurityRequirement(name = "bearerAuth")
public class ActivationRequestController {

    static final String RATE_LIMITER = "requestsApi";

    private final CreateActivationUseCase createActivation;
    private final FindActivationUseCase findActivation;

    public ActivationRequestController(CreateActivationUseCase createActivation, FindActivationUseCase findActivation) {
        this.createActivation = createActivation;
        this.findActivation = findActivation;
    }

    @PostMapping
    @Operation(summary = "Create an activation request")
    @ApiResponse(responseCode = "201", description = "Request created")
    @ApiResponse(responseCode = "200", description = "Same externalId and same data: the existing request is returned")
    @ApiResponse(responseCode = "409", description = "Same externalId with different data")
    public ResponseEntity<ActivationRequestDto> create(@Valid @RequestBody CreateActivationRequestDto body,
                                                       @AuthenticationPrincipal Jwt jwt) {
        CreateActivationResult result = createActivation.create(
                body.toCommand(clientId(jwt), MDC.get(CorrelationIdFilter.MDC_KEY)));
        ActivationRequestDto response = ActivationRequestDto.from(result.request());

        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find an activation request by id")
    public ActivationRequestDto findById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ActivationRequestDto.from(findActivation.findById(id, clientId(jwt)));
    }

    @GetMapping
    @Operation(summary = "List activation requests of the caller, newest first")
    public PageDto<ActivationRequestDto> search(@RequestParam(required = false) ActivationStatus status,
                                                @RequestParam(required = false) String customerId,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                @AuthenticationPrincipal Jwt jwt) {
        var search = new ActivationSearch(clientId(jwt), status, customerId, page, size);
        return PageDto.from(findActivation.search(search), ActivationRequestDto::from);
    }

    /**
     * In the client credentials flow the calling system is identified by "client_id" (or "azp");
     * "sub" is the fallback for authorization servers that send neither of them.
     */
    private static String clientId(Jwt jwt) {
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId == null) {
            clientId = jwt.getClaimAsString("azp");
        }
        return clientId != null ? clientId : jwt.getSubject();
    }
}
