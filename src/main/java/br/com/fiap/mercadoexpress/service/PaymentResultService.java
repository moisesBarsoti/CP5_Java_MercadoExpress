
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.entity.OrderItem;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.mercadoexpress.event.OrderPaidEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentResultService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approvePayment(Long orderId) {

        Order order = findOrderForUpdate(orderId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn(
                    "Ignoring approval for order {} with status {}",
                    orderId,
                    order.getStatus()
            );
            return;
        }

        order.markAsPaid();

        eventPublisher.publishEvent(
                new OrderPaidEvent(orderId)
        );

        log.info("Order {} payment approved", orderId);
    }

    @Transactional
    public void rejectPayment(Long orderId) {

        Order order = findOrderForUpdate(orderId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn(
                    "Ignoring rejection for order {} with status {}",
                    orderId,
                    order.getStatus()
            );
            return;
        }

        Map<Long, Integer> quantities = new TreeMap<>();

        for (OrderItem item : order.getItems()) {
            quantities.merge(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    Math::addExact
            );
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {

            Product product = productRepository
                    .findByIdForUpdate(entry.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found"
                            )
                    );

            product.restoreStock(entry.getValue());
        }

        order.markAsPaymentFailed();

        log.info(
                "Order {} payment rejected; stock restored",
                orderId
        );
    }

    private Order findOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );
    }
}
