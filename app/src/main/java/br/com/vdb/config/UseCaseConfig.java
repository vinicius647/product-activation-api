package br.com.vdb.config;

import br.com.vdb.core.port.out.ActivationEventPublisher;
import br.com.vdb.core.port.out.ActivationProviderPort;
import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.core.service.ActivationDispatchService;
import br.com.vdb.core.service.ActivationRequestService;
import br.com.vdb.core.service.DispatchSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the framework-free core services as Spring beans.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ActivationRequestService activationRequestService(ActivationRepositoryPort repository, Clock clock) {
        return new ActivationRequestService(repository, clock);
    }

    @Bean
    ActivationDispatchService activationDispatchService(ActivationRepositoryPort repository,
                                                        ActivationProviderPort provider,
                                                        ActivationEventPublisher eventPublisher,
                                                        Clock clock,
                                                        ActivationProperties properties) {
        var settings = new DispatchSettings(
                properties.dispatch().batchSize(),
                properties.reconciliation().stuckAfter(),
                properties.retry().toPolicy());
        return new ActivationDispatchService(repository, provider, eventPublisher, clock, settings);
    }
}
