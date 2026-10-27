package br.com.vdb.config;

import br.com.vdb.core.port.out.ActivationRepositoryPort;
import br.com.vdb.core.service.ProductActivationService;
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
    ProductActivationService productActivationService(ActivationRepositoryPort repository, Clock clock) {
        return new ProductActivationService(repository, clock);
    }
}
