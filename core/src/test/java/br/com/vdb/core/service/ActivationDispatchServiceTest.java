package br.com.vdb.core.service;

import br.com.vdb.core.domain.event.ActivationFinishedEvent;
import br.com.vdb.core.domain.exception.ConcurrentUpdateException;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.domain.model.RetryPolicy;
import br.com.vdb.core.port.out.ActivationEventPublisher;
import br.com.vdb.core.port.out.ActivationProviderPort;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.core.port.out.ProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivationDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final RetryPolicy RETRY = new RetryPolicy(3, Duration.ofSeconds(2), Duration.ofMinutes(5), 0);

    @Mock
    private ActivationRepositoryPort repository;
    @Mock
    private ActivationProviderPort provider;
    @Mock
    private ActivationEventPublisher publisher;

    private ActivationDispatchService service;

    @BeforeEach
    void setUp() {
        var settings = new DispatchSettings(50, Duration.ofMinutes(5), RETRY);
        service = new ActivationDispatchService(repository, provider, publisher, Clock.fixed(NOW, ZoneOffset.UTC), settings);
    }

    @Test
    void completesAndPublishesEventWhenProviderSucceeds() {
        ActivationRequest claimed = claimed(1);
        when(provider.activate(claimed)).thenReturn(new ProviderResult.Success());

        service.dispatch(claimed);

        assertThat(saved().status()).isEqualTo(ActivationStatus.COMPLETED);
        ActivationFinishedEvent event = published();
        assertThat(event.status()).isEqualTo(ActivationStatus.COMPLETED);
        assertThat(event.reason()).isNull();
    }

    @Test
    void failsWithoutRetryWhenProviderRejects() {
        ActivationRequest claimed = claimed(1);
        when(provider.activate(claimed)).thenReturn(new ProviderResult.PermanentFailure("HTTP 400"));

        service.dispatch(claimed);

        assertThat(saved().status()).isEqualTo(ActivationStatus.FAILED);
        assertThat(published().reason()).isEqualTo("HTTP 400");
    }

    @Test
    void schedulesNewAttemptOnTemporaryFailure() {
        ActivationRequest claimed = claimed(2);
        when(provider.activate(claimed)).thenReturn(new ProviderResult.TemporaryFailure("HTTP 500", null));

        service.dispatch(claimed);

        ActivationRequest saved = saved();
        assertThat(saved.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(saved.nextAttemptAt()).isEqualTo(NOW.plusSeconds(4));
        assertThat(saved.lastError()).isEqualTo("HTTP 500");
        verify(publisher, never()).publish(any());
    }

    @Test
    void respectsRetryAfterFromProvider() {
        ActivationRequest claimed = claimed(1);
        when(provider.activate(claimed))
                .thenReturn(new ProviderResult.TemporaryFailure("HTTP 503", Duration.ofSeconds(30)));

        service.dispatch(claimed);

        assertThat(saved().nextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void failsAndPublishesEventWhenAttemptsAreOver() {
        ActivationRequest claimed = claimed(3);
        when(provider.activate(claimed)).thenReturn(new ProviderResult.TemporaryFailure("timeout", null));

        service.dispatch(claimed);

        assertThat(saved().status()).isEqualTo(ActivationStatus.FAILED);
        ActivationFinishedEvent event = published();
        assertThat(event.status()).isEqualTo(ActivationStatus.FAILED);
        assertThat(event.reason()).contains("3 attempts").contains("timeout");
    }

    @Test
    void doesNotPublishWhenAnotherProcessChangedTheRequest() {
        ActivationRequest claimed = claimed(1);
        when(provider.activate(claimed)).thenReturn(new ProviderResult.Success());
        when(repository.update(any())).thenThrow(new ConcurrentUpdateException(claimed.id()));

        service.dispatch(claimed);

        verify(publisher, never()).publish(any());
    }

    @Test
    void releasesStuckRequestsForANewAttempt() {
        ActivationRequest stuck = claimed(1);
        when(repository.findStuck(NOW.minus(Duration.ofMinutes(5)), 50)).thenReturn(List.of(stuck));

        int released = service.releaseStuck();

        assertThat(released).isEqualTo(1);
        assertThat(saved().status()).isEqualTo(ActivationStatus.PENDING);
    }

    /** A request in PROCESSING after {@code attempts} calls. */
    private static ActivationRequest claimed(int attempts) {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "client-a", null,
                NOW.minusSeconds(60));
        for (int i = 1; i < attempts; i++) {
            request = request.startProcessing(NOW).retryLater("previous error", NOW, NOW);
        }
        return request.startProcessing(NOW);
    }

    private ActivationRequest saved() {
        ArgumentCaptor<ActivationRequest> captor = ArgumentCaptor.forClass(ActivationRequest.class);
        verify(repository).update(captor.capture());
        return captor.getValue();
    }

    private ActivationFinishedEvent published() {
        ArgumentCaptor<ActivationFinishedEvent> captor = ArgumentCaptor.forClass(ActivationFinishedEvent.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }
}
