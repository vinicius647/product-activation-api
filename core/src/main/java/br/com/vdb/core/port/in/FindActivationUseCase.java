package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ActivationRequest;

import java.util.UUID;

public interface FindActivationUseCase {

    /** Returns the request only if it belongs to {@code clientId}. */
    ActivationRequest findById(UUID id, String clientId);

    PageResult<ActivationRequest> search(ActivationSearch search);
}
