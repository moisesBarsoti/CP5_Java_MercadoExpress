
package br.com.fiap.mercadoexpress.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_QUEUE =
            "mercadoexpress.payment.queue";

    public static final String PAYMENT_EXCHANGE =
            "mercadoexpress.payment.exchange";

    public static final String PAYMENT_ROUTING_KEY =
            "payment.requested";

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding paymentBinding(
            Queue paymentQueue,
            DirectExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(paymentExchange)
                .with(PAYMENT_ROUTING_KEY);
    }
}
