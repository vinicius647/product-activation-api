package br.com.vdb.core.port.out;

import br.com.vdb.core.domain.event.ActivationFinishedEvent;

public interface ActivationEventPublisher {

    void publish(ActivationFinishedEvent event);
}
