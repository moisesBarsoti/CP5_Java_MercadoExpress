
package br.com.fiap.mercadoexpress.messaging;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publishPaymentRequest(OrderCreatedEvent event) {

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_ROUTING_KEY,
                    event.orderId().toString(),
                    message -> {
                        message.getMessageProperties()
                                .setDeliveryMode(
                                        MessageDeliveryMode.PERSISTENT
                                );

                        return message;
                    }
            );

            log.info(
                    "Payment request published for order {}",
                    event.orderId()
            );

        } catch (AmqpException exception) {
            log.error(
                    "Failed to publish payment request for order {}",
                    event.orderId(),
                    exception
            );
        }
    }
}