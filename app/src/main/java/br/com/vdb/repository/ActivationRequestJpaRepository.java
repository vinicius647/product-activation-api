package br.com.vdb.repository;

import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.entity.ActivationRequestEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationRequestJpaRepository extends JpaRepository<ActivationRequestEntity, UUID> {

    Optional<ActivationRequestEntity> findByExternalId(String externalId);

    @Query("""
            select r from ActivationRequestEntity r
            where r.clientId = :clientId
              and (:status is null or r.status = :status)
              and (:customerId is null or r.customerId = :customerId)
            """)
    Page<ActivationRequestEntity> search(String clientId, ActivationStatus status, String customerId, Pageable pageable);

    /**
     * Locks the due PENDING requests. SKIP LOCKED makes other instances skip these rows
     * instead of waiting for them, so each instance works on a different batch.
     * Must run inside a transaction.
     */
    @Query(value = """
            SELECT * FROM activation_request
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ActivationRequestEntity> lockDueRequests(Instant now, int limit);

    List<ActivationRequestEntity> findByStatusAndUpdatedAtBefore(ActivationStatus status, Instant before, Limit limit);

    long countByStatus(ActivationStatus status);
}
