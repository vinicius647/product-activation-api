package br.com.vdb.adapter;

import br.com.vdb.core.domain.model.ProductActivation;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.entity.ProductActivationEntity;
import br.com.vdb.repository.ProductActivationJpaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter implementing {@link ActivationRepositoryPort} with JPA.
 * Calls are protected by the "persistence" circuit breaker.
 */
@Component
@CircuitBreaker(name = ProductActivationPersistenceAdapter.CIRCUIT_BREAKER)
public class ProductActivationPersistenceAdapter implements ActivationRepositoryPort {

    static final String CIRCUIT_BREAKER = "persistence";

    private final ProductActivationJpaRepository repository;

    public ProductActivationPersistenceAdapter(ProductActivationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductActivation save(ProductActivation activation) {
        return repository.save(ProductActivationEntity.fromDomain(activation)).toDomain();
    }

    @Override
    public Optional<ProductActivation> findById(UUID id) {
        return repository.findById(id).map(ProductActivationEntity::toDomain);
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        return repository.existsByExternalId(externalId);
    }
}
