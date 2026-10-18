
package br.com.fiap.mercadoexpress.messaging;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.dto.bank.BankPaymentRequest;
import br.com.fiap.mercadoexpress.dto.bank.BankPaymentResponse;
import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class PaymentRequestConsumer {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestClient bankClient;
    private final JsonMapper jsonMapper;


    public PaymentRequestConsumer(
            OrderRepository orderRepository,
            RabbitTemplate rabbitTemplate,
            JsonMapper jsonMapper,
            @Value("${bank.api.base-url}") String bankBaseUrl
    ) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;

        this.bankClient = RestClient.builder()
                .baseUrl(bankBaseUrl)
                .build();
    }


    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void consume(String message) {

        Long orderId = Long.valueOf(message);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.info("Order {} is already processed", orderId);
            return;
        }

        BankPaymentRequest request = new BankPaymentRequest(
                order.getId(),
                order.getTotalAmount()
        );

        BankPaymentResponse response = bankClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(BankPaymentResponse.class);

        if (response == null
                || !orderId.equals(response.orderId())
                || response.transactionId() == null
                || response.transactionId().isBlank()) {
            throw new IllegalStateException(
                    "Invalid bank payment response"
            );
        }

        String routingKey = switch (response.status()) {
            case "APPROVED" ->
                    RabbitMQConfig.PAYMENT_APPROVED_ROUTING_KEY;

            case "DECLINED" ->
                    RabbitMQConfig.PAYMENT_REJECTED_ROUTING_KEY;

            default -> throw new IllegalStateException(
                    "Unknown payment status"
            );
        };

        String responseJson = jsonMapper.writeValueAsString(response);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                routingKey,
                responseJson,
                outgoingMessage -> {
                    outgoingMessage.getMessageProperties()
                            .setDeliveryMode(
                                    MessageDeliveryMode.PERSISTENT
                            );

                    return outgoingMessage;
                }
        );

        log.info(
                "Bank processed order {} with status {}",
                orderId,
                response.status()
        );
    }
}
