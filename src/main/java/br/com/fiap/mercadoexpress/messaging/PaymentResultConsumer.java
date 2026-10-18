
package br.com.fiap.mercadoexpress.messaging;

import br.com.fiap.mercadoexpress.config.RabbitMQConfig;
import br.com.fiap.mercadoexpress.dto.bank.BankPaymentResponse;
import br.com.fiap.mercadoexpress.service.PaymentResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final PaymentResultService paymentResultService;
    private final JsonMapper jsonMapper;

    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_APPROVED_QUEUE
    )
    public void consumeApproved(String message) throws Exception {

        BankPaymentResponse response = jsonMapper.readValue(
                message,
                BankPaymentResponse.class
        );

        validateResponse(response, "APPROVED");

        paymentResultService.approvePayment(response.orderId());

        log.info(
                "Approved payment response processed for order {}",
                response.orderId()
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_REJECTED_QUEUE
    )
    public void consumeRejected(String message) throws Exception {

        BankPaymentResponse response = jsonMapper.readValue(
                message,
                BankPaymentResponse.class
        );

        validateResponse(response, "DECLINED");

        paymentResultService.rejectPayment(response.orderId());

        log.info(
                "Rejected payment response processed for order {}",
                response.orderId()
        );
    }

    private void validateResponse(
            BankPaymentResponse response,
            String expectedStatus
    ) {
        if (response == null
                || response.orderId() == null
                || response.orderId() <= 0
                || response.transactionId() == null
                || response.transactionId().isBlank()
                || !expectedStatus.equals(response.status())) {

            throw new IllegalArgumentException(
                    "Invalid bank payment response"
            );
        }
    }
}
