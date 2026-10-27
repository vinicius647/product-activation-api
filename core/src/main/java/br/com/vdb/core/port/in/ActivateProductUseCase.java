package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ProductActivation;

public interface ActivateProductUseCase {

    ProductActivation activate(ActivateProductCommand command);
}
