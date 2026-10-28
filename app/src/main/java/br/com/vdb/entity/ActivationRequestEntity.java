package br.com.vdb.entity;

import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.domain.model.ActivationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activation_request")
public class ActivationRequestEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, updatable = false, length = 64)
    private String externalId;

    @Column(name = "customer_id", nullable = false, updatable = false, length = 64)
    private String customerId;

    @Column(name = "product_code", nullable = false, updatable = false, length = 64)
    private String productCode;

    @Column(name = "client_id", nullable = false, updatable = false, length = 100)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    // A null version tells Spring Data the entity is new, so save() runs an INSERT instead of a merge.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActivationRequestEntity() {
    }

    public static ActivationRequestEntity fromDomain(ActivationRequest request) {
        ActivationRequestEntity entity = new ActivationRequestEntity();
        entity.id = request.id();
        entity.externalId = request.externalId();
        entity.customerId = request.customerId();
        entity.productCode = request.productCode();
        entity.clientId = request.clientId();
        entity.correlationId = request.correlationId();
        entity.version = request.version();
        entity.createdAt = request.createdAt();
        entity.applyState(request);
        return entity;
    }

    /** Copies the fields that change during processing. Id, version and creation data are never copied. */
    public void applyState(ActivationRequest request) {
        this.status = request.status();
        this.attempts = request.attempts();
        this.nextAttemptAt = request.nextAttemptAt();
        this.lastError = request.lastError();
        this.updatedAt = request.updatedAt();
    }

    public ActivationRequest toDomain() {
        return new ActivationRequest(id, externalId, customerId, productCode, clientId, status, attempts,
                nextAttemptAt, lastError, correlationId, version, createdAt, updatedAt);
    }
}
