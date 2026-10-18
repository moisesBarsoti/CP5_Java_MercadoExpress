
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.entity.OutboxEvent;
import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import br.com.fiap.mercadoexpress.repository.OutboxEventRepository;
import br.com.fiap.mercadoexpress.service.OutboxEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxEventProcessor processor;

    @Test
    void shouldPublishEventAfterRabbitMqConfirmation() {

        OutboxEvent event = OutboxEvent.paymentRequested(100L);

        when(outboxEventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        mockConfirmation(true);

        processor.process(1L);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(0, event.getAttempts());
    }

    @Test
    void shouldKeepEventPendingWhenRabbitMqRejectsPublication() {

        OutboxEvent event = OutboxEvent.paymentRequested(100L);

        when(outboxEventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        mockConfirmation(false);

        processor.process(1L);

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertNull(event.getPublishedAt());
    }

    @Test
    void shouldNotPublishAlreadyPublishedEvent() {

        OutboxEvent event = OutboxEvent.paymentRequested(100L);
        event.markAsPublished();

        when(outboxEventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        processor.process(1L);

        verifyNoInteractions(rabbitTemplate);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    }

    private void mockConfirmation(boolean ack) {

        doAnswer(invocation -> {

            CorrelationData correlationData =
                    invocation.getArgument(4);

            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(
                            ack,
                            ack ? null : "Publication rejected"
                    )
            );

            return null;

        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_ROUTING_KEY),
                eq("100"),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }
}
