package br.com.vdb.core.service;

import br.com.vdb.core.domain.exception.ActivationAlreadyExistsException;
import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.domain.model.ProductActivation;
import br.com.vdb.core.port.in.ActivateProductCommand;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductActivationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private ProductActivationService service;

    @BeforeEach
    void setUp() {
        service = new ProductActivationService(new InMemoryRepository(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPendingActivation() {
        ProductActivation activation = service.activate(
                new ActivateProductCommand("EXT-1", "CUST-1", "INTERNET-500"));

        assertThat(activation.status()).isEqualTo(ActivationStatus.PENDING);
        assertThat(activation.externalId()).isEqualTo("EXT-1");
        assertThat(activation.productCode()).isEqualTo("INTERNET-500");
        assertThat(activation.createdAt()).isEqualTo(NOW);
        assertThat(activation.updatedAt()).isEqualTo(NOW);
        assertThat(service.findById(activation.id())).isEqualTo(activation);
    }

    @Test
    void rejectsDuplicateExternalId() {
        service.activate(new ActivateProductCommand("EXT-1", "CUST-1", "INTERNET-500"));

        assertThatThrownBy(() -> service.activate(new ActivateProductCommand("EXT-1", "CUST-2", "TV-HD")))
                .isInstanceOf(ActivationAlreadyExistsException.class);
    }

    @Test
    void failsWhenActivationDoesNotExist() {
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ActivationNotFoundException.class);
    }

    private static class InMemoryRepository implements ActivationRepositoryPort {

        private final Map<UUID, ProductActivation> store = new HashMap<>();

        @Override
        public ProductActivation save(ProductActivation activation) {
            store.put(activation.id(), activation);
            return activation;
        }

        @Override
        public Optional<ProductActivation> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean existsByExternalId(String externalId) {
            return store.values().stream().anyMatch(a -> a.externalId().equals(externalId));
        }
    }
}
