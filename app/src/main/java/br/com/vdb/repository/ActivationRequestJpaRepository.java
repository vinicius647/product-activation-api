package br.com.vdb.repository;

import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.entity.ActivationRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
