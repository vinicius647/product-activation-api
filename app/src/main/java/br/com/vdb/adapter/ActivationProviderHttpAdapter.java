package br.com.vdb.adapter;

import br.com.vdb.config.CorrelationIdFilter;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.out.ActivationProviderPort;
import br.com.vdb.core.port.out.ProviderResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Calls POST /provider/v1/activate. The request id is sent as Idempotency-Key, so a retry after
 * a timeout (when the provider may have activated the product already) does not activate it twice.
 * <p>
 * Calls go through the "provider" circuit breaker: when the provider keeps failing, calls stop
 * for a while and requests are scheduled for a new attempt without waiting for timeouts.
 */
@Component
public class ActivationProviderHttpAdapter implements ActivationProviderPort {

    static final String ACTIVATE_PATH = "/provider/v1/activate";
    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final Logger log = LoggerFactory.getLogger(ActivationProviderHttpAdapter.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Clock clock;

    public ActivationProviderHttpAdapter(RestClient providerRestClient, CircuitBreakerRegistry circuitBreakers,
                                         Clock clock) {
        this.restClient = providerRestClient;
        this.circuitBreaker = circuitBreakers.circuitBreaker("provider");
        this.clock = clock;
    }

    @Override
    public ProviderResult activate(ActivationRequest request) {
        try {
            circuitBreaker.executeRunnable(() -> send(request));
            log.info("Provider activated request {}", request.id());
            return new ProviderResult.Success();
        } catch (CallNotPermittedException e) {
            return temporary(request, "Provider circuit breaker is open", null);
        } catch (RestClientResponseException e) {
            return fromErrorResponse(request, e);
        } catch (ResourceAccessException e) {
            // Timeout or connection error. The provider may have received the call: the retry uses the same key.
            return temporary(request, "Provider did not answer: " + e.getMostSpecificCause().getClass().getSimpleName(),
                    null);
        }
    }

    private void send(ActivationRequest request) {
        restClient.post()
                .uri(ACTIVATE_PATH)
                .header(IDEMPOTENCY_KEY, request.id().toString())
                .headers(headers -> {
                    if (request.correlationId() != null) {
                        headers.set(CorrelationIdFilter.HEADER, request.correlationId());
                    }
                })
                .body(new ProviderActivationBody(request.id(), request.externalId(), request.customerId(),
                        request.productCode()))
                .retrieve()
                .toBodilessEntity();
    }

    private ProviderResult fromErrorResponse(ActivationRequest request, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String reason = "Provider answered HTTP " + status;
        return switch (status) {
            case 408, 429, 503 -> temporary(request, reason, retryAfter(e.getResponseHeaders()));
            default -> {
                if (status >= 500) {
                    yield temporary(request, reason, null);
                }
                log.warn("Provider rejected request {}: {}", request.id(), reason);
                yield new ProviderResult.PermanentFailure(reason);
            }
        };
    }

    private static ProviderResult temporary(ActivationRequest request, String reason, Duration retryAfter) {
        log.warn("Temporary provider failure for request {}: {}", request.id(), reason);
        return new ProviderResult.TemporaryFailure(reason, retryAfter);
    }

    /** Retry-After can be a number of seconds or an HTTP date. Invalid values are ignored. */
    Duration retryAfter(HttpHeaders headers) {
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Math.max(Long.parseLong(value.trim()), 0));
        } catch (NumberFormatException notSeconds) {
            try {
                ZonedDateTime date = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                Duration wait = Duration.between(clock.instant(), date.toInstant());
                return wait.isNegative() ? Duration.ZERO : wait;
            } catch (DateTimeParseException notADate) {
                return null;
            }
        }
    }

    record ProviderActivationBody(UUID requestId, String externalId, String customerId, String productCode) {
    }
}
