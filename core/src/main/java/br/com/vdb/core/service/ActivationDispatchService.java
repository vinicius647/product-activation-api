package br.com.vdb.core.service;

import br.com.vdb.core.domain.event.ActivationFinishedEvent;
import br.com.vdb.core.domain.exception.ConcurrentUpdateException;
import br.com.vdb.core.domain.model.ActivationRequest;
import br.com.vdb.core.port.in.DispatchActivationUseCase;
import br.com.vdb.core.port.out.ActivationEventPublisher;
import br.com.vdb.core.port.out.ActivationProviderPort;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.core.port.out.ProviderResult;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sends requests to the provider and applies the result. The provider call never runs inside
 * a database transaction: the request is claimed and committed first, and the result is saved after.
 */
public class ActivationDispatchService implements DispatchActivationUseCase {

    private static final Logger LOG = System.getLogger(ActivationDispatchService.class.getName());

    private final ActivationRepositoryPort repository;
    private final ActivationProviderPort provider;
    private final ActivationEventPublisher eventPublisher;
    private final Clock clock;
    private final DispatchSettings settings;

    public ActivationDispatchService(ActivationRepositoryPort repository, ActivationProviderPort provider,
                                     ActivationEventPublisher eventPublisher, Clock clock, DispatchSettings settings) {
        this.repository = repository;
        this.provider = provider;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public List<ActivationRequest> claimPending() {
        return repository.claimPending(settings.batchSize(), clock.instant());
    }

    @Override
    public void dispatch(ActivationRequest claimed) {
        ProviderResult result = provider.activate(claimed);
        Instant now = clock.instant();

        ActivationRequest next = switch (result) {
            case ProviderResult.Success success -> claimed.complete(now);
            case ProviderResult.PermanentFailure failure -> claimed.fail(failure.reason(), now);
            case ProviderResult.TemporaryFailure failure ->
                    retryOrFail(claimed, failure.reason(), failure.retryAfter(), now);
        };
        save(next);
    }

    @Override
    public int releaseStuck() {
        Instant now = clock.instant();
        List<ActivationRequest> stuck =
                repository.findStuck(now.minus(settings.stuckAfter()), settings.batchSize());

        int released = 0;
        for (ActivationRequest request : stuck) {
            String reason = "No provider result saved after " + settings.stuckAfter();
            if (save(retryOrFail(request, reason, null, now))) {
                released++;
            }
        }
        return released;
    }

    private ActivationRequest retryOrFail(ActivationRequest request, String reason, Duration retryAfter, Instant now) {
        if (!settings.retryPolicy().canRetry(request.attempts())) {
            return request.fail("Gave up after " + request.attempts() + " attempts. Last error: " + reason, now);
        }
        Duration delay = settings.retryPolicy().nextDelay(request.attempts(), retryAfter);
        return request.retryLater(reason, now.plus(delay), now);
    }

    /** Returns false when another process changed the request first; its change wins. */
    private boolean save(ActivationRequest next) {
        try {
            repository.update(next);
        } catch (ConcurrentUpdateException e) {
            LOG.log(Level.WARNING, "Result for request {0} was not saved: {1}", next.id(), e.getMessage());
            return false;
        }
        if (next.status().isFinal()) {
            eventPublisher.publish(ActivationFinishedEvent.from(next));
        }
        return true;
    }
}
