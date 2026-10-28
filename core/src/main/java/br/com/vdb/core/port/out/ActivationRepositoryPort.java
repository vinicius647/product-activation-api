package br.com.vdb.core.port.out;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.ActivationSearch;
import br.com.vdb.core.port.in.PageResult;

import java.time.Instant;
import java.util.List;
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

    /**
     * In one short transaction: locks up to {@code limit} PENDING requests whose next attempt is due,
     * skipping rows already locked by other instances, moves them to PROCESSING and commits.
     */
    List<ActivationRequest> claimPending(int limit, Instant now);

    /** Requests in PROCESSING that were not updated since {@code processingBefore}. */
    List<ActivationRequest> findStuck(Instant processingBefore, int limit);

    /**
     * Saves a status change.
     *
     * @throws br.com.vdb.core.domain.exception.ConcurrentUpdateException if the row changed after it was read
     */
    ActivationRequest update(ActivationRequest request);
}
