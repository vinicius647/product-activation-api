package br.com.vdb.core.domain.exception;

/**
 * The externalId is already used by a request with different data.
 */
public class DuplicateActivationException extends DomainException {

    public DuplicateActivationException(String externalId) {
        super("externalId " + externalId + " was already used with different data");
    }
}
