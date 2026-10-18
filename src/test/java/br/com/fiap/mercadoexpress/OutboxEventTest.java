
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.entity.OutboxEvent;
import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void shouldCreatePendingPaymentEvent() {

        OutboxEvent event =
                OutboxEvent.paymentRequested(10L);

        assertEquals(10L, event.getOrderId());

        assertEquals("10", event.getPayload());

        assertEquals(
                OutboxStatus.PENDING,
                event.getStatus()
        );

        assertEquals(0, event.getAttempts());

        assertNotNull(event.getCreatedAt());

        assertNull(event.getPublishedAt());
    }

    @Test
    void shouldRejectInvalidOrderId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> OutboxEvent.paymentRequested(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> OutboxEvent.paymentRequested(0L)
        );
    }

    @Test
    void shouldRegisterPublicationFailure() {

        OutboxEvent event =
                OutboxEvent.paymentRequested(11L);

        event.recordFailure();
        event.recordFailure();

        assertEquals(2, event.getAttempts());

        assertEquals(
                OutboxStatus.PENDING,
                event.getStatus()
        );
    }

    @Test
    void shouldMarkEventAsPublished() {

        OutboxEvent event =
                OutboxEvent.paymentRequested(12L);

        event.markAsPublished();

        assertEquals(
                OutboxStatus.PUBLISHED,
                event.getStatus()
        );

        assertNotNull(event.getPublishedAt());
    }

    @Test
    void shouldPreventPublishingEventTwice() {

        OutboxEvent event =
                OutboxEvent.paymentRequested(13L);

        event.markAsPublished();

        assertThrows(
                IllegalStateException.class,
                event::markAsPublished
        );

        assertThrows(
                IllegalStateException.class,
                event::recordFailure
        );
    }
}
