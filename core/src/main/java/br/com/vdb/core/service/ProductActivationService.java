package br.com.vdb.core.service;

import br.com.vdb.core.domain.exception.ActivationAlreadyExistsException;
import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import br.com.vdb.core.domain.model.ProductActivation;
import br.com.vdb.core.port.in.ActivateProductCommand;
import br.com.vdb.core.port.in.ActivateProductUseCase;
import br.com.vdb.core.port.in.FindActivationUseCase;
import br.com.vdb.core.port.out.ActivationRepositoryPort;

import java.time.Clock;
import java.util.UUID;

/**
 * Implements the inbound ports. Framework-free: wired as a bean by the app module.
 */
public class ProductActivationService implements ActivateProductUseCase, FindActivationUseCase {

    private final ActivationRepositoryPort repository;
    private final Clock clock;

    public ProductActivationService(ActivationRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public ProductActivation activate(ActivateProductCommand command) {
        if (repository.existsByExternalId(command.externalId())) {
            throw new ActivationAlreadyExistsException(command.externalId());
        }
        return repository.save(ProductActivation.create(
                command.externalId(), command.customerId(), command.productCode(), clock.instant()));
    }

    @Override
    public ProductActivation findById(UUID activationId) {
        return repository.findById(activationId)
                .orElseThrow(() -> new ActivationNotFoundException(activationId));
    }
}
