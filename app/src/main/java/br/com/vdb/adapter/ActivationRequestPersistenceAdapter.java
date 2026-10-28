package br.com.vdb.adapter;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.ActivationSearch;
import br.com.vdb.core.port.in.PageResult;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.entity.ActivationRequestEntity;
import br.com.vdb.repository.ActivationRequestJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implements {@link ActivationRepositoryPort} with Spring Data JPA.
 */
@Component
public class ActivationRequestPersistenceAdapter implements ActivationRepositoryPort {

    private final ActivationRequestJpaRepository repository;

    public ActivationRequestPersistenceAdapter(ActivationRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ActivationRequest> insertIfAbsent(ActivationRequest request) {
        try {
            return Optional.of(repository.saveAndFlush(ActivationRequestEntity.fromDomain(request)).toDomain());
        } catch (DataIntegrityViolationException e) {
            // The only constraint that valid input can break is the unique external_id.
            return Optional.empty();
        }
    }

    @Override
    public Optional<ActivationRequest> findById(UUID id) {
        return repository.findById(id).map(ActivationRequestEntity::toDomain);
    }

    @Override
    public Optional<ActivationRequest> findByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(ActivationRequestEntity::toDomain);
    }

    @Override
    public PageResult<ActivationRequest> search(ActivationSearch search) {
        var pageable = PageRequest.of(search.page(), search.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivationRequestEntity> page =
                repository.search(search.clientId(), search.status(), search.customerId(), pageable);
        return new PageResult<>(page.map(ActivationRequestEntity::toDomain).getContent(),
                search.page(), search.size(), page.getTotalElements());
    }
}
