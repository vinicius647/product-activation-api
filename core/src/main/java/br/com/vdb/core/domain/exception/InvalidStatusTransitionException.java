package br.com.vdb.core.domain.exception;

import br.com.vdb.core.domain.model.ActivationStatus;

import java.util.UUID;

public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(UUID id, ActivationStatus from, ActivationStatus to) {
        super("Activation request " + id + " cannot change from " + from + " to " + to);
    }
}
