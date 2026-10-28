package br.com.vdb.adapter;

import br.com.vdb.core.domain.event.ActivationFinishedEvent;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.port.in.CreateActivationCommand;
import br.com.vdb.core.port.in.CreateActivationUseCase;
import br.com.vdb.core.port.in.DispatchActivationUseCase;
import br.com.vdb.core.port.in.FindActivationUseCase;
import br.com.vdb.core.port.out.ActivationEventPublisher;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Full dispatch flow against a fake provider (WireMock): claim with SKIP LOCKED, HTTP call,
 * status update with optimistic lock and event publishing.
 */
@SpringBootTest(properties = {
        "app.activation.scheduler.enabled=false",
        // Own database, so requests created by other test classes are not dispatched here.
        "spring.datasource.url=jdbc:h2:mem:dispatchtest;DB_CLOSE_DELAY=-1",
        "app.provider.read-timeout=500ms",
        "app.activation.retry.initial-backoff=10m",
        "app.activation.retry.max-backoff=1h",
        "app.activation.retry.jitter=0"
})
class ActivationDispatchIntegrationTest {

    private static final String PATH = "/provider/v1/activate";
    private static final String CLIENT = "client-a";

    private static final WireMockServer provider = new WireMockServer(options().dynamicPort());

    static {
        provider.start();
    }

    @DynamicPropertySource
    static void providerUrl(DynamicPropertyRegistry registry) {
        registry.add("app.provider.base-url", provider::baseUrl);
    }

    @Autowired
    private CreateActivationUseCase createActivation;
    @Autowired
    private DispatchActivationUseCase dispatchActivation;
    @Autowired
    private FindActivationUseCase findActivation;
    @Autowired
    private CircuitBreakerRegistry circuitBreakers;
    @MockitoSpyBean
    private ActivationEventPublisher eventPublisher;

    @AfterAll
    static void stopProvider() {
        provider.stop();
    }

    @BeforeEach
    void reset() {
        provider.resetAll();
        circuitBreakers.circuitBreaker("provider").reset();
    }

    @Test
    void completesWhenProviderSucceeds() {
        ActivationRequest request = create("OK-PRODUCT");
        stubProvider("OK-PRODUCT", ok());

        dispatchDueRequests();

        assertThat(statusOf(request)).isEqualTo(ActivationStatus.COMPLETED);
        provider.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Idempotency-Key", equalTo(request.id().toString()))
                .withHeader("X-Correlation-Id", equalTo("corr-" + request.externalId()))
                .withRequestBody(matchingJsonPath("$.externalId", equalTo(request.externalId()))));
        verify(eventPublisher).publish(argThat(event -> isEvent(event, request, ActivationStatus.COMPLETED)));
    }

    @Test
    void failsWithoutRetryWhenProviderReturns400() {
        ActivationRequest request = create("BAD-PRODUCT");
        stubProvider("BAD-PRODUCT", aResponse().withStatus(400));

        dispatchDueRequests();

        ActivationRequest saved = reload(request);
        assertThat(saved.status()).isEqualTo(ActivationStatus.FAILED);
        assertThat(saved.lastError()).contains("400");
        verify(eventPublisher).publish(argThat(event -> isEvent(event, request, ActivationStatus.FAILED)));
    }

    @Test
    void schedulesRetryWhenProviderReturns500() {
        ActivationRequest request = create("ERROR-PRODUCT");
        stubProvider("ERROR-PRODUCT", aResponse().withStatus(500));
        Instant before = Instant.now();

        dispatchDueRequests();

        ActivationRequest saved = reload(request);
        assertThat(saved.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(saved.attempts()).isEqualTo(1);
        assertThat(saved.nextAttemptAt()).isAfterOrEqualTo(before.plus(Duration.ofMinutes(10)));
        verify(eventPublisher, never()).publish(argThat(event -> event.requestId().equals(request.id())));
    }

    @Test
    void schedulesRetryWhenProviderTimesOut() {
        ActivationRequest request = create("SLOW-PRODUCT");
        stubProvider("SLOW-PRODUCT", ok().withFixedDelay(1_500));

        dispatchDueRequests();

        ActivationRequest saved = reload(request);
        assertThat(saved.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(saved.lastError()).contains("did not answer");
    }

    @Test
    void waitsForRetryAfterWhenProviderIsBusy() {
        ActivationRequest request = create("BUSY-PRODUCT");
        stubProvider("BUSY-PRODUCT", aResponse().withStatus(503).withHeader("Retry-After", "1200"));
        Instant before = Instant.now();

        dispatchDueRequests();

        ActivationRequest saved = reload(request);
        assertThat(saved.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(saved.nextAttemptAt()).isAfterOrEqualTo(before.plus(Duration.ofMinutes(20)));
    }

    private ActivationRequest create(String productCode) {
        String externalId = "EXT-" + UUID.randomUUID();
        return createActivation.create(new CreateActivationCommand(externalId, "CUST-1", productCode, CLIENT,
                "corr-" + externalId)).request();
    }

    private static void stubProvider(String productCode, ResponseDefinitionBuilder response) {
        provider.stubFor(post(urlEqualTo(PATH))
                .withRequestBody(matchingJsonPath("$.productCode", equalTo(productCode)))
                .willReturn(response));
    }

    private void dispatchDueRequests() {
        dispatchActivation.claimPending().forEach(dispatchActivation::dispatch);
    }

    private ActivationRequest reload(ActivationRequest request) {
        return findActivation.findById(request.id(), CLIENT);
    }

    private ActivationStatus statusOf(ActivationRequest request) {
        return reload(request).status();
    }

    private static boolean isEvent(ActivationFinishedEvent event, ActivationRequest request, ActivationStatus status) {
        return event.requestId().equals(request.id()) && event.status() == status;
    }
}
