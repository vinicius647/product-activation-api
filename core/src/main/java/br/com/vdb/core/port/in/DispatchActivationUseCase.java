package br.com.vdb.core.port.in;

import br.com.vdb.core.domain.model.ActivationRequest;

import java.util.List;

/**
 * Sends pending requests to the provider. Called by the schedulers.
 */
public interface DispatchActivationUseCase {

    /** Claims a batch of due requests (now PROCESSING). Each one must then be passed to {@link #dispatch}. */
    List<ActivationRequest> claimPending();

    /** Calls the provider for a claimed request and saves the new status. */
    void dispatch(ActivationRequest claimed);

    /**
     * Reconciliation: requests stuck in PROCESSING (for example, the instance stopped during the call)
     * go back to PENDING, or to FAILED when there are no attempts left.
     *
     * @return how many requests were released
     */
    int releaseStuck();
}
