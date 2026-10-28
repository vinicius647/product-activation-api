package br.com.vdb.core.service;

import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import br.com.vdb.core.domain.exception.DuplicateActivationException;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.ActivationSearch;
import br.com.vdb.core.port.in.CreateActivationCommand;
import br.com.vdb.core.port.in.CreateActivationResult;
import br.com.vdb.core.port.in.CreateActivationUseCase;
import br.com.vdb.core.port.in.FindActivationUseCase;
import br.com.vdb.core.port.in.PageResult;
import br.com.vdb.core.port.out.ActivationRepositoryPort;

import java.time.Clock;
import java.util.UUID;

/**
 * Creates and reads activation requests. Framework-free: wired as a bean by the app module.
 */
public class ActivationRequestService implements CreateActivationUseCase, FindActivationUseCase {

    private final ActivationRepositoryPort repository;
    private final Clock clock;

    public ActivationRequestService(ActivationRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * The first lookup is only a shortcut for client retries. The unique constraint on externalId
     * is what really prevents duplicates: if two equal requests pass the lookup at the same time,
     * only one insert succeeds and the other one reads the winner.
     */
    @Override
    public CreateActivationResult create(CreateActivationCommand command) {
        var existing = repository.findByExternalId(command.externalId());
        if (existing.isPresent()) {
            return resolveRepeated(existing.get(), command);
        }

        var request = ActivationRequest.create(command.externalId(), command.customerId(), command.productCode(),
                command.clientId(), command.correlationId(), clock.instant());

        return repository.insertIfAbsent(request)
                .map(inserted -> new CreateActivationResult(inserted, true))
                .orElseGet(() -> resolveRepeated(repository.findByExternalId(command.externalId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Insert was rejected but externalId " + command.externalId() + " was not found")),
                        command));
    }

    @Override
    public ActivationRequest findById(UUID id, String clientId) {
        // A request of another client is reported as not found, so its existence is not leaked.
        return repository.findById(id)
                .filter(request -> request.clientId().equals(clientId))
                .orElseThrow(() -> new ActivationNotFoundException(id));
    }

    @Override
    public PageResult<ActivationRequest> search(ActivationSearch search) {
        return repository.search(search);
    }

    private static CreateActivationResult resolveRepeated(ActivationRequest existing, CreateActivationCommand command) {
        if (existing.isSameRequest(command.clientId(), command.customerId(), command.productCode())) {
            return new CreateActivationResult(existing, false);
        }
        throw new DuplicateActivationException(command.externalId());
    }
}
