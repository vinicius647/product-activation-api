package br.com.vdb.core.domain.exception;

import java.util.UUID;

public class ActivationNotFoundException extends DomainException {

    public ActivationNotFoundException(UUID id) {
        super("Activation " + id + " not found");
    }
}
