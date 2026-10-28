package br.com.vdb.core.domain.exception;

import java.util.UUID;

/**
 * The request was changed by someone else after it was read (optimistic lock).
 */
public class ConcurrentUpdateException extends DomainException {

    public ConcurrentUpdateException(UUID id) {
        super("Activation request " + id + " was changed by another process");
    }
}
