package br.com.vdb.core.domain.exception;

public class ActivationAlreadyExistsException extends DomainException {

    public ActivationAlreadyExistsException(String externalId) {
        super("Activation with externalId " + externalId + " already exists");
    }
}
