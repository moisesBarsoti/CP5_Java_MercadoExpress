
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.dto.bank.BankPaymentRequest;
import br.com.fiap.mercadoexpress.dto.bank.BankPaymentResponse;
import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.messaging.PaymentRequestConsumer;
import br.com.fiap.mercadoexpress.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestConsumerTest {

    private static final Long ORDER_ID = 7L;

    private static final BigDecimal ORDER_AMOUNT =
            new BigDecimal("6400.00");

    private static final String PAYMENT_JSON =
            "{\"transactionId\":\"BT-123\"," +
                    "\"orderId\":7," +
                    "\"status\":\"APPROVED\"," +
                    "\"message\":\"Payment processed\"}";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private RestClient bankClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PaymentRequestConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRequestConsumer(
                orderRepository,
                rabbitTemplate,
                jsonMapper,
                "http://localhost:8081"
        );

        ReflectionTestUtils.setField(
                consumer,
                "bankClient",
                bankClient
        );
    }

    @Test
    void shouldPublishApprovedPayment() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        BankPaymentResponse response =
                validBankResponse("APPROVED");

        mockBankResponse(response);

        when(jsonMapper.writeValueAsString(response))
                .thenReturn(PAYMENT_JSON);

        consumer.consume(ORDER_ID.toString());

        verifyPaymentPublished(
                RabbitMQConfig.PAYMENT_APPROVED_ROUTING_KEY
        );

        ArgumentCaptor<BankPaymentRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        BankPaymentRequest.class
                );

        verify(requestSpec).body(requestCaptor.capture());

        BankPaymentRequest bankRequest =
                requestCaptor.getValue();

        assertEquals(ORDER_ID, bankRequest.orderId());

        assertEquals(
                0,
                ORDER_AMOUNT.compareTo(bankRequest.amount())
        );
    }

    @Test
    void shouldPublishDeclinedPayment() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        BankPaymentResponse response =
                validBankResponse("DECLINED");

        mockBankResponse(response);

        when(jsonMapper.writeValueAsString(response))
                .thenReturn(PAYMENT_JSON);

        consumer.consume(ORDER_ID.toString());

        verifyPaymentPublished(
                RabbitMQConfig.PAYMENT_REJECTED_ROUTING_KEY
        );
    }

    @Test
    void shouldIgnoreAlreadyProcessedOrder() {

        mockOrder(OrderStatus.PAID);

        consumer.consume(ORDER_ID.toString());

        verify(orderRepository)
                .findById(ORDER_ID);

        verifyNoInteractions(
                bankClient,
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldThrowExceptionWhenBankIsUnavailable() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        mockBankRequest();

        when(responseSpec.body(BankPaymentResponse.class))
                .thenThrow(
                        new RestClientException(
                                "Bank service unavailable"
                        )
                );

        assertThrows(
                RestClientException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        verifyNoInteractions(
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldRejectResponseWithDifferentOrderId() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        BankPaymentResponse response =
                mock(BankPaymentResponse.class);

        when(response.orderId()).thenReturn(99L);

        mockBankResponse(response);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        assertEquals(
                "Invalid bank payment response",
                exception.getMessage()
        );

        verifyNoInteractions(
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldRejectUnknownPaymentStatus() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        BankPaymentResponse response =
                validBankResponse("UNKNOWN");

        mockBankResponse(response);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        assertEquals(
                "Unknown payment status",
                exception.getMessage()
        );

        verifyNoInteractions(
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldRejectNullBankResponse() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        mockBankResponse(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        assertEquals(
                "Invalid bank payment response",
                exception.getMessage()
        );

        verifyNoInteractions(
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldRejectResponseWithoutTransactionId() {

        mockOrder(OrderStatus.PENDING_PAYMENT);

        BankPaymentResponse response =
                mock(BankPaymentResponse.class);

        when(response.orderId()).thenReturn(ORDER_ID);
        when(response.transactionId()).thenReturn(" ");

        mockBankResponse(response);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        assertEquals(
                "Invalid bank payment response",
                exception.getMessage()
        );

        verifyNoInteractions(
                rabbitTemplate,
                jsonMapper
        );
    }

    @Test
    void shouldRejectPaymentWhenOrderDoesNotExist() {

        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> consumer.consume(ORDER_ID.toString())
        );

        assertEquals(
                "Order not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                bankClient,
                rabbitTemplate,
                jsonMapper
        );
    }

    private void mockOrder(OrderStatus status) {

        Order order = mock(Order.class);

        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus()).thenReturn(status);

        if (status == OrderStatus.PENDING_PAYMENT) {

            when(order.getId()).thenReturn(ORDER_ID);

            when(order.getTotalAmount())
                    .thenReturn(ORDER_AMOUNT);
        }
    }

    private BankPaymentResponse validBankResponse(
            String status
    ) {

        BankPaymentResponse response =
                mock(BankPaymentResponse.class);

        when(response.orderId()).thenReturn(ORDER_ID);

        when(response.transactionId())
                .thenReturn("BT-123");

        when(response.status()).thenReturn(status);

        return response;
    }

    private void mockBankRequest() {

        when(bankClient.post())
                .thenReturn(requestSpec);

        when(requestSpec.uri("/api/payments"))
                .thenReturn(requestSpec);

        when(requestSpec.contentType(
                MediaType.APPLICATION_JSON
        )).thenReturn(requestSpec);

        when(requestSpec.body(
                any(BankPaymentRequest.class)
        )).thenReturn(requestSpec);

        when(requestSpec.retrieve())
                .thenReturn(responseSpec);
    }

    private void mockBankResponse(
            BankPaymentResponse response
    ) {

        mockBankRequest();

        when(responseSpec.body(BankPaymentResponse.class))
                .thenReturn(response);
    }

    private void verifyPaymentPublished(
            String expectedRoutingKey
    ) {

        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(
                        MessagePostProcessor.class
                );

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(expectedRoutingKey),
                eq(PAYMENT_JSON),
                processorCaptor.capture()
        );

        Message message = new Message(
                "{}".getBytes(StandardCharsets.UTF_8),
                new MessageProperties()
        );

        Message processedMessage =
                processorCaptor.getValue()
                        .postProcessMessage(message);

        assertEquals(
                MessageDeliveryMode.PERSISTENT,
                processedMessage
                        .getMessageProperties()
                        .getDeliveryMode()
        );
    }
}
