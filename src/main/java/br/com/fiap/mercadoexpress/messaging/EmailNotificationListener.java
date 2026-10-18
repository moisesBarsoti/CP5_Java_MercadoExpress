
package br.com.fiap.mercadoexpress.messaging;

import br.com.fiap.mercadoexpress.event.OrderPaidEvent;
import br.com.fiap.mercadoexpress.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailNotificationService emailNotificationService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onOrderPaid(OrderPaidEvent event) {

        try {
            emailNotificationService.sendOrderConfirmation(
                    event.orderId()
            );

        } catch (Exception exception) {
            log.error(
                    "Error processing email notification for order {}",
                    event.orderId(),
                    exception
            );
        }
    }
}
