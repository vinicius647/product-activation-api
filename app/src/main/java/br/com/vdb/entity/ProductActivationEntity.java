package br.com.vdb.entity;

import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.core.domain.model.ProductActivation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_activation")
public class ProductActivationEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductActivationEntity() {
    }

    public static ProductActivationEntity fromDomain(ProductActivation activation) {
        ProductActivationEntity entity = new ProductActivationEntity();
        entity.id = activation.id();
        entity.externalId = activation.externalId();
        entity.customerId = activation.customerId();
        entity.productCode = activation.productCode();
        entity.status = activation.status();
        entity.createdAt = activation.createdAt();
        entity.updatedAt = activation.updatedAt();
        return entity;
    }

    public ProductActivation toDomain() {
        return new ProductActivation(id, externalId, customerId, productCode, status, createdAt, updatedAt);
    }
}
