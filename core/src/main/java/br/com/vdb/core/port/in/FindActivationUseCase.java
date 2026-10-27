package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ProductActivation;

import java.util.UUID;

public interface FindActivationUseCase {

    ProductActivation findById(UUID activationId);
}
