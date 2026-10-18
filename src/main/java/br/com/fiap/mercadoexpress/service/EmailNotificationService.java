
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.entity.EmailLog;
import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.enums.EmailStatus;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.repository.EmailLogRepository;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final OrderRepository orderRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOrderConfirmation(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        if (order.getStatus() != OrderStatus.PAID) {
            return;
        }

        if (emailLogRepository.existsByOrder_IdAndStatus(
                orderId,
                EmailStatus.SENT
        )) {
            log.info("Email already sent for order {}", orderId);
            return;
        }

        String recipient = order.getCustomer().getEmail();
        String subject = "MercadoExpress - Payment Confirmation";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(
                "Hello, " + order.getCustomer().getName() + "!\n\n"
                        + "Your payment has been approved.\n\n"
                        + "Order: #" + order.getId() + "\n"
                        + "Total: R$ " + order.getTotalAmount() + "\n"
                        + "Status: PAID\n\n"
                        + "Thank you for shopping at MercadoExpress!"
        );

        EmailStatus status;
        String errorMessage = null;

        try {
            mailSender.send(message);
            status = EmailStatus.SENT;

            log.info("Confirmation email sent for order {}", orderId);

        } catch (MailException exception) {
            status = EmailStatus.FAILED;

            String detail = exception.getMessage();
            errorMessage = detail == null
                    ? "Unknown mail error"
                    : detail.substring(0, Math.min(detail.length(), 1000));

            log.error(
                    "Failed to send email for order {}",
                    orderId,
                    exception
            );
        }

        emailLogRepository.save(
                EmailLog.builder()
                        .order(order)
                        .recipientEmail(recipient)
                        .subject(subject)
                        .status(status)
                        .errorMessage(errorMessage)
                        .build()
        );
    }
}
