
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config =
            new RabbitMQConfig();

    @Test
    void shouldCreateDurableDeadLetterQueue() {

        Queue queue = config.paymentDeadLetterQueue();

        assertEquals(
                RabbitMQConfig.PAYMENT_DEAD_LETTER_QUEUE,
                queue.getName()
        );

        assertTrue(queue.isDurable());
    }

    @Test
    void shouldBindDeadLetterQueueToExchange() {

        Binding binding = config.paymentDeadLetterBinding(
                config.paymentDeadLetterQueue(),
                config.paymentDeadLetterExchange()
        );

        assertEquals(
                RabbitMQConfig.PAYMENT_DEAD_LETTER_EXCHANGE,
                binding.getExchange()
        );

        assertEquals(
                RabbitMQConfig.PAYMENT_DEAD_LETTER_ROUTING_KEY,
                binding.getRoutingKey()
        );

        assertEquals(
                RabbitMQConfig.PAYMENT_DEAD_LETTER_QUEUE,
                binding.getDestination()
        );
    }

    @Test
    void shouldKeepExistingPaymentQueuesDurable() {

        assertTrue(config.paymentQueue().isDurable());

        assertTrue(
                config.paymentApprovedQueue().isDurable()
        );

        assertTrue(
                config.paymentRejectedQueue().isDurable()
        );
    }
}
