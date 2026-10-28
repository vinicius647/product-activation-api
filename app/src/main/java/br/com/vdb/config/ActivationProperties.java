package br.com.vdb.config;

import br.com.vdb.core.domain.model.RetryPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Settings of the processing of activation requests ({@code app.activation.*}).
 */
@Validated
@ConfigurationProperties("app.activation")
public record ActivationProperties(@Valid @NotNull Dispatch dispatch,
                                   @Valid @NotNull Reconciliation reconciliation,
                                   @Valid @NotNull Retry retry) {

    public record Dispatch(@Min(1) int batchSize, @NotNull Duration fixedDelay) {
    }

    public record Reconciliation(@NotNull Duration fixedDelay, @NotNull Duration stuckAfter) {
    }

    public record Retry(@Min(1) int maxAttempts,
                        @NotNull Duration initialBackoff,
                        @NotNull Duration maxBackoff,
                        @DecimalMin("0.0") @DecimalMax("1.0") double jitter) {

        public RetryPolicy toPolicy() {
            return new RetryPolicy(maxAttempts, initialBackoff, maxBackoff, jitter);
        }
    }
}
