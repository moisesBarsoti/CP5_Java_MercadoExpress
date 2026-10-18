
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.entity.User;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.event.OrderPaidEvent;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import br.com.fiap.mercadoexpress.service.PaymentResultService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentResultServiceTest {

    private static final Long ORDER_ID = 5L;
    private static final Long PRODUCT_ID = 2L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentResultService paymentResultService;

    @BeforeEach
    void setUp() {
        paymentResultService = new PaymentResultService(
                orderRepository,
                productRepository,
                eventPublisher
        );
    }

    @Test
    void shouldApprovePendingPayment() {

        Order order = createPendingOrder();

        mockOrder(order);

        paymentResultService.approvePayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAID,
                order.getStatus()
        );

        verify(orderRepository)
                .findByIdForUpdate(ORDER_ID);

        verify(eventPublisher, times(1))
                .publishEvent(any(OrderPaidEvent.class));

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldRejectPaymentAndRestoreStock() {

        Product product = createProduct(8);

        Order order = createPendingOrder();
        order.addItem(product, 2);

        mockOrder(order);

        when(productRepository.findByIdForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(product));

        paymentResultService.rejectPayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAYMENT_FAILED,
                order.getStatus()
        );

        assertEquals(
                10,
                product.getStockQuantity()
        );

        verify(productRepository, times(1))
                .findByIdForUpdate(PRODUCT_ID);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldIgnoreDuplicateApproval() {

        Order order = createPendingOrder();

        mockOrder(order);

        paymentResultService.approvePayment(ORDER_ID);
        paymentResultService.approvePayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAID,
                order.getStatus()
        );

        verify(orderRepository, times(2))
                .findByIdForUpdate(ORDER_ID);

        verify(eventPublisher, times(1))
                .publishEvent(any(OrderPaidEvent.class));

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldIgnoreDuplicateRejection() {

        Product product = createProduct(8);

        Order order = createPendingOrder();
        order.addItem(product, 2);

        mockOrder(order);

        when(productRepository.findByIdForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(product));

        paymentResultService.rejectPayment(ORDER_ID);
        paymentResultService.rejectPayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAYMENT_FAILED,
                order.getStatus()
        );

        assertEquals(
                10,
                product.getStockQuantity()
        );

        verify(orderRepository, times(2))
                .findByIdForUpdate(ORDER_ID);

        verify(productRepository, times(1))
                .findByIdForUpdate(PRODUCT_ID);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldIgnoreApprovalAfterPaymentRejection() {

        Order order = createPendingOrder();

        order.markAsPaymentFailed();

        mockOrder(order);

        paymentResultService.approvePayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAYMENT_FAILED,
                order.getStatus()
        );

        verifyNoInteractions(
                productRepository,
                eventPublisher
        );
    }

    @Test
    void shouldIgnoreRejectionAfterPaymentApproval() {

        Order order = createPendingOrder();

        order.markAsPaid();

        mockOrder(order);

        paymentResultService.rejectPayment(ORDER_ID);

        assertEquals(
                OrderStatus.PAID,
                order.getStatus()
        );

        verifyNoInteractions(
                productRepository,
                eventPublisher
        );
    }

    @Test
    void shouldRejectPaymentWhenOrderDoesNotExist() {

        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentResultService.approvePayment(
                        ORDER_ID
                )
        );

        assertEquals(
                "Order not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                productRepository,
                eventPublisher
        );
    }

    @Test
    void shouldKeepOrderPendingWhenProductIsNotFound() {

        Product product = createProduct(8);

        Order order = createPendingOrder();
        order.addItem(product, 2);

        mockOrder(order);

        when(productRepository.findByIdForUpdate(PRODUCT_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentResultService.rejectPayment(
                        ORDER_ID
                )
        );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        assertEquals(
                OrderStatus.PENDING_PAYMENT,
                order.getStatus()
        );

        assertEquals(
                8,
                product.getStockQuantity()
        );

        verifyNoInteractions(eventPublisher);
    }

    private Order createPendingOrder() {

        User customer = mock(User.class);

        Order order = Order.builder()
                .customer(customer)
                .build();

        ReflectionTestUtils.setField(
                order,
                "id",
                ORDER_ID
        );

        return order;
    }

    private Product createProduct(int stockQuantity) {

        Product product = Product.builder()
                .name("Notebook Dell Inspiron")
                .price(new BigDecimal("3200.00"))
                .costPrice(new BigDecimal("2500.00"))
                .stockQuantity(stockQuantity)
                .build();

        ReflectionTestUtils.setField(
                product,
                "id",
                PRODUCT_ID
        );

        return product;
    }

    private void mockOrder(Order order) {

        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));
    }
}
