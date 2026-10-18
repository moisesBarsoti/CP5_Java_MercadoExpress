
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.entity.OutboxEvent;
import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import br.com.fiap.mercadoexpress.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void process(Long eventId) {

        OutboxEvent event = outboxEventRepository
                .findByIdForUpdate(eventId)
                .orElse(null);

        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        CorrelationData correlationData = new CorrelationData(
                "outbox-" + eventId + "-" + UUID.randomUUID()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_ROUTING_KEY,
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties()
                                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                        message.getMessageProperties()
                                .setMessageId(
                                        "payment-request-" + event.getOrderId()
                                );

                        return message;
                    },
                    correlationData
            );

            CorrelationData.Confirm confirm = correlationData
                    .getFuture()
                    .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                event.recordFailure();

                log.warn(
                        "RabbitMQ rejected outbox event {}: {}",
                        eventId,
                        confirm.getReason()
                );

                return;
            }

            if (correlationData.getReturned() != null) {
                event.recordFailure();

                log.warn(
                        "Outbox event {} could not be routed",
                        eventId
                );

                return;
            }

            event.markAsPublished();

            log.info(
                    "Outbox event {} published for order {}",
                    eventId,
                    event.getOrderId()
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            event.recordFailure();

            log.error(
                    "Outbox publishing interrupted for event {}",
                    eventId,
                    exception
            );

        } catch (Exception exception) {

            event.recordFailure();

            log.error(
                    "Failed to publish outbox event {}",
                    eventId,
                    exception
            );
        }
    }
}
