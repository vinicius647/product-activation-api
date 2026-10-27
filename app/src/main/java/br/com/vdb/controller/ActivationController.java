package br.com.vdb.controller;

import br.com.vdb.controller.dto.ActivationRequest;
import br.com.vdb.controller.dto.ActivationResponse;
import br.com.vdb.core.port.in.ActivateProductUseCase;
import br.com.vdb.core.port.in.FindActivationUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Inbound REST adapter. All endpoints share the "activationApi" rate limiter.
 */
@RestController
@RequestMapping("/api/v1/activations")
@RateLimiter(name = ActivationController.RATE_LIMITER)
@Tag(name = "Activations", description = "Product activation management")
@SecurityRequirement(name = "bearerAuth")
public class ActivationController {

    static final String RATE_LIMITER = "activationApi";

    private final ActivateProductUseCase activateProduct;
    private final FindActivationUseCase findActivation;

    public ActivationController(ActivateProductUseCase activateProduct, FindActivationUseCase findActivation) {
        this.activateProduct = activateProduct;
        this.findActivation = findActivation;
    }

    @PostMapping
    @Operation(summary = "Activate a product")
    public ResponseEntity<ActivationResponse> activate(@Valid @RequestBody ActivationRequest request) {
        ActivationResponse response = ActivationResponse.from(activateProduct.activate(request.toCommand()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find an activation by id")
    public ActivationResponse findById(@PathVariable UUID id) {
        return ActivationResponse.from(findActivation.findById(id));
    }
}
