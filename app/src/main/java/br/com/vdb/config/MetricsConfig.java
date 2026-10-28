package br.com.vdb.config;

import br.com.vdb.core.domain.model.ActivationStatus;
import br.com.vdb.repository.ActivationRequestJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * activation_requests{status="..."}: number of requests in each status, read from the database
     * when the metrics are scraped. Uses the (status, next_attempt_at) index.
     */
    @Bean
    MeterBinder activationRequestsByStatus(ActivationRequestJpaRepository repository) {
        return registry -> {
            for (ActivationStatus status : ActivationStatus.values()) {
                Gauge.builder("activation.requests", repository, repo -> repo.countByStatus(status))
                        .description("Activation requests by status")
                        .tag("status", status.name())
                        .register(registry);
            }
        };
    }
}
