package br.com.vdb.core.port.out;

import br.com.vdb.core.domain.model.ActivationRequest;

public interface ActivationProviderPort {

    /**
     * Sends the request to the external provider. Implementations must send an idempotency key
     * (the request id), so a retry after a timeout does not activate the product twice.
     * Never throws: every outcome is returned as a {@link ProviderResult}.
     */
    ProviderResult activate(ActivationRequest request);
}
