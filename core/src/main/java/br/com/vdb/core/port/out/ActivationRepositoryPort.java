package br.com.vdb.core.port.out;

import br.com.vdb.core.domain.model.ProductActivation;

import java.util.Optional;
import java.util.UUID;

public interface ActivationRepositoryPort {

    ProductActivation save(ProductActivation activation);

    Optional<ProductActivation> findById(UUID id);

    boolean existsByExternalId(String externalId);
}
