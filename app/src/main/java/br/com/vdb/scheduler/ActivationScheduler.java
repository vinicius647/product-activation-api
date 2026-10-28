package br.com.vdb.scheduler;

import br.com.vdb.config.CorrelationIdFilter;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.DispatchActivationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the dispatch and the reconciliation. Every instance of the application runs both:
 * the row locks (SKIP LOCKED) and the optimistic lock keep them from processing the same request.
 * Disabled with {@code app.activation.scheduler.enabled=false} (used by tests).
 */
@Component
@ConditionalOnProperty(name = "app.activation.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ActivationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivationScheduler.class);

    private final DispatchActivationUseCase dispatchActivation;

    public ActivationScheduler(DispatchActivationUseCase dispatchActivation) {
        this.dispatchActivation = dispatchActivation;
    }

    /**
     * fixedDelay: the next run starts only after this one ends, so runs never overlap.
     * The claimed batch is sent in parallel, one virtual thread per request; the method waits for all of them.
     */
    @Scheduled(fixedDelayString = "${app.activation.dispatch.fixed-delay}")
    public void dispatchPending() {
        List<ActivationRequest> claimed = dispatchActivation.claimPending();
        if (claimed.isEmpty()) {
            return;
        }
        log.debug("Dispatching {} activation requests", claimed.size());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            claimed.forEach(request -> executor.submit(() -> dispatch(request)));
        }
    }

    @Scheduled(fixedDelayString = "${app.activation.reconciliation.fixed-delay}",
            initialDelayString = "${app.activation.reconciliation.fixed-delay}")
    public void releaseStuck() {
        int released = dispatchActivation.releaseStuck();
        if (released > 0) {
            log.warn("Reconciliation released {} requests stuck in PROCESSING", released);
        }
    }

    private void dispatch(ActivationRequest request) {
        // Same correlation id as the HTTP request that created it, so the whole flow can be found in the logs.
        if (request.correlationId() != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, request.correlationId());
        }
        try {
            dispatchActivation.dispatch(request);
        } catch (RuntimeException e) {
            // The request stays in PROCESSING and the reconciliation sends it again later.
            log.error("Unexpected error while dispatching request {}", request.id(), e);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
