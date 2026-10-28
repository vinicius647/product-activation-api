package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ActivationRequest;

/**
 * @param created false when the request already existed with the same data (a client retry)
 */
public record CreateActivationResult(ActivationRequest request, boolean created) {
}
