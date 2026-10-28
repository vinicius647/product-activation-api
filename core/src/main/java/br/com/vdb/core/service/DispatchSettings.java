package br.com.vdb.core.service;

import br.com.vdb.core.domain.model.RetryPolicy;

import java.time.Duration;

/**
 * @param batchSize  how many requests are claimed per scheduler run
 * @param stuckAfter time in PROCESSING after which a request is considered stuck
 */
public record DispatchSettings(int batchSize, Duration stuckAfter, RetryPolicy retryPolicy) {
}
