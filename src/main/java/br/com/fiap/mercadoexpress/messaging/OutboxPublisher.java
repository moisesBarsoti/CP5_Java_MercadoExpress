
package br.com.fiap.mercadoexpress.messaging;

import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import br.com.fiap.mercadoexpress.repository.OutboxEventRepository;
import br.com.fiap.mercadoexpress.service.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.outbox.enabled",
        havingValue = "true"
)
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(
            fixedDelayString = "${app.outbox.poll-interval-ms:5000}"
    )
    public void publishPendingEvents() {

        var events = outboxEventRepository
                .findTop20ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                );

        for (var event : events) {

            try {
                outboxEventProcessor.process(event.getId());

            } catch (Exception exception) {
                log.error(
                        "Unexpected error processing outbox event {}",
                        event.getId(),
                        exception
                );
            }
        }
    }
}
