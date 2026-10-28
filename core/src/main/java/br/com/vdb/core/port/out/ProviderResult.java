package br.com.vdb.core.port.out;

import java.time.Duration;

/**
 * Outcome of a call to the activation provider.
 */
public sealed interface ProviderResult {

    /** The provider activated the product. */
    record Success() implements ProviderResult {
    }

    /** The provider refused the request. Sending it again will not help. */
    record PermanentFailure(String reason) implements ProviderResult {
    }

    /**
     * The call may work later (server error, timeout, provider busy).
     *
     * @param retryAfter wait time asked by the provider, or null
     */
    record TemporaryFailure(String reason, Duration retryAfter) implements ProviderResult {
    }
}
