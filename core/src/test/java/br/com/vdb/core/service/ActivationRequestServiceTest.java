package br.com.vdb.core.service;

import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import br.com.vdb.core.domain.exception.DuplicateActivationException;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.port.in.CreateActivationCommand;
import br.com.vdb.core.port.in.CreateActivationResult;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivationRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final CreateActivationCommand COMMAND =
            new CreateActivationCommand("EXT-1", "CUST-1", "INTERNET-500", "client-a", "corr-1");

    @Mock
    private ActivationRepositoryPort repository;

    private ActivationRequestService service;

    @BeforeEach
    void setUp() {
        service = new ActivationRequestService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPendingRequest() {
        when(repository.findByExternalId("EXT-1")).thenReturn(Optional.empty());
        when(repository.insertIfAbsent(any())).thenAnswer(call -> Optional.of(call.getArgument(0)));

        CreateActivationResult result = service.create(COMMAND);

        assertThat(result.created()).isTrue();
        assertThat(result.request().status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(result.request().clientId()).isEqualTo("client-a");
        assertThat(result.request().createdAt()).isEqualTo(NOW);
    }

    @Test
    void returnsExistingRequestWhenClientRetriesWithSameData() {
        ActivationRequest existing = existing("client-a", "INTERNET-500");
        when(repository.findByExternalId("EXT-1")).thenReturn(Optional.of(existing));

        CreateActivationResult result = service.create(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.request()).isEqualTo(existing);
        verify(repository, never()).insertIfAbsent(any());
    }

    @Test
    void rejectsSameExternalIdWithDifferentData() {
        when(repository.findByExternalId("EXT-1")).thenReturn(Optional.of(existing("client-a", "TV-HD")));

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(DuplicateActivationException.class);
    }

    @Test
    void rejectsSameExternalIdFromAnotherClient() {
        when(repository.findByExternalId("EXT-1")).thenReturn(Optional.of(existing("client-b", "INTERNET-500")));

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(DuplicateActivationException.class);
    }

    @Test
    void returnsWinnerWhenAParallelRequestInsertedFirst() {
        ActivationRequest winner = existing("client-a", "INTERNET-500");
        when(repository.findByExternalId("EXT-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.insertIfAbsent(any())).thenReturn(Optional.empty());

        CreateActivationResult result = service.create(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.request()).isEqualTo(winner);
    }

    @Test
    void hidesRequestsOfOtherClients() {
        ActivationRequest existing = existing("client-b", "INTERNET-500");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.findById(existing.id(), "client-a"))
                .isInstanceOf(ActivationNotFoundException.class);
    }

    private static ActivationRequest existing(String clientId, String productCode) {
        return ActivationRequest.create("EXT-1", "CUST-1", productCode, clientId, "corr-0", NOW.minusSeconds(60));
    }
}
