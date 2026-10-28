package br.com.vdb.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Connection to the external activation provider ({@code app.provider.*}).
 */
@Validated
@ConfigurationProperties("app.provider")
public record ProviderProperties(@NotBlank String baseUrl,
                                 @NotNull Duration connectTimeout,
                                 @NotNull Duration readTimeout) {
}
