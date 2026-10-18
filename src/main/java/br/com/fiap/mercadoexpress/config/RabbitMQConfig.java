
package br.com.fiap.mercadoexpress.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_QUEUE =
            "mercadoexpress.payment.queue";

    public static final String PAYMENT_APPROVED_QUEUE =
            "mercadoexpress.payment.approved.queue";

    public static final String PAYMENT_REJECTED_QUEUE =
            "mercadoexpress.payment.rejected.queue";

    public static final String PAYMENT_EXCHANGE =
            "mercadoexpress.payment.exchange";

    public static final String PAYMENT_ROUTING_KEY =
            "payment.requested";

    public static final String PAYMENT_APPROVED_ROUTING_KEY =
            "payment.approved";

    public static final String PAYMENT_REJECTED_ROUTING_KEY =
            "payment.rejected";

    public static final String PAYMENT_DEAD_LETTER_EXCHANGE =
            "mercadoexpress.payment.dlx";

    public static final String PAYMENT_DEAD_LETTER_QUEUE =
            "mercadoexpress.payment.dlq";

    public static final String PAYMENT_DEAD_LETTER_ROUTING_KEY =
            "payment.failed";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(
                PAYMENT_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public DirectExchange paymentDeadLetterExchange() {
        return new DirectExchange(
                PAYMENT_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return new Queue(PAYMENT_APPROVED_QUEUE, true);
    }

    @Bean
    public Queue paymentRejectedQueue() {
        return new Queue(PAYMENT_REJECTED_QUEUE, true);
    }

    @Bean
    public Queue paymentDeadLetterQueue() {
        return new Queue(PAYMENT_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding paymentBinding(
            @Qualifier("paymentQueue") Queue queue,
            @Qualifier("paymentExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding paymentApprovedBinding(
            @Qualifier("paymentApprovedQueue") Queue queue,
            @Qualifier("paymentExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(PAYMENT_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRejectedBinding(
            @Qualifier("paymentRejectedQueue") Queue queue,
            @Qualifier("paymentExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(PAYMENT_REJECTED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentDeadLetterBinding(
            @Qualifier("paymentDeadLetterQueue") Queue queue,
            @Qualifier("paymentDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(PAYMENT_DEAD_LETTER_ROUTING_KEY);
    }
}
