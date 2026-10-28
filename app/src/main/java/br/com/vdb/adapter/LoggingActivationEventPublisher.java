package br.com.vdb.adapter;

import br.com.vdb.core.domain.event.ActivationFinishedEvent;
import br.com.vdb.core.port.out.ActivationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulates publishing the event to a message broker by writing it to the log.
 * To use a real broker, add another implementation of {@link ActivationEventPublisher}
 * (for example, one that sends to a RabbitMQ exchange) and remove this one.
 */
@Component
public class LoggingActivationEventPublisher implements ActivationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingActivationEventPublisher.class);

    @Override
    public void publish(ActivationFinishedEvent event) {
        log.info("Event ActivationFinished published: requestId={} externalId={} clientId={} status={} reason={}",
                event.requestId(), event.externalId(), event.clientId(), event.status(), event.reason());
    }
}
