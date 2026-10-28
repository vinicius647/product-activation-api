package br.com.vdb.core.port.out;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.ActivationSearch;
import br.com.vdb.core.port.in.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface ActivationRepositoryPort {

    /**
     * Inserts a new request. Returns empty when another request with the same externalId
     * already exists. This must be enforced by the database (unique constraint), not by a previous read.
     */
    Optional<ActivationRequest> insertIfAbsent(ActivationRequest request);

    Optional<ActivationRequest> findById(UUID id);

    Optional<ActivationRequest> findByExternalId(String externalId);

    PageResult<ActivationRequest> search(ActivationSearch search);
}
