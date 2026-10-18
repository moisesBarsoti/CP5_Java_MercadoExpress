
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.dto.request.OrderItemRequest;
import br.com.fiap.mercadoexpress.dto.request.OrderRequest;
import br.com.fiap.mercadoexpress.dto.response.OrderResponse;
import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.entity.OutboxEvent;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.entity.User;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.OutboxEventRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import br.com.fiap.mercadoexpress.repository.UserRepository;
import br.com.fiap.mercadoexpress.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderService orderService;

    private static final String CUSTOMER_EMAIL =
            "customer@test.com";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productRepository,
                userRepository,
                outboxEventRepository
        );
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        mockCustomer();
        mockSavedOrder();

        Product product = createProduct(12);

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(product));

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(2L, 1))
        );

        OrderResponse response = orderService.createOrder(
                request,
                CUSTOMER_EMAIL
        );

        assertNotNull(response);
        assertEquals(5L, response.id());
        assertEquals(
                OrderStatus.PENDING_PAYMENT,
                response.status()
        );

        assertEquals(
                0,
                new BigDecimal("3200.00")
                        .compareTo(response.totalAmount())
        );

        assertEquals(1, response.items().size());
        assertEquals(1, response.items().get(0).quantity());
        assertEquals(11, product.getStockQuantity());

        verify(orderRepository)
                .saveAndFlush(any(Order.class));

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        mockCustomer();

        Product product = createProduct(1);

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(product));

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(2L, 2))
        );

        assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(
                        request,
                        CUSTOMER_EMAIL
                )
        );

        assertEquals(1, product.getStockQuantity());

        verify(orderRepository, never())
                .saveAndFlush(any(Order.class));

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        mockCustomer();

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(2L, 0))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(
                        request,
                        CUSTOMER_EMAIL
                )
        );

        assertEquals(
                "Invalid order item",
                exception.getMessage()
        );

        verifyNoInteractions(
                productRepository,
                orderRepository,
                outboxEventRepository
        );
    }

    @Test
    void shouldMergeDuplicateProductsIntoOneOrderItem() {
        mockCustomer();
        mockSavedOrder();

        Product product = createProduct(12);

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(product));

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(2L, 1),
                        new OrderItemRequest(2L, 2)
                )
        );

        OrderResponse response = orderService.createOrder(
                request,
                CUSTOMER_EMAIL
        );

        assertEquals(1, response.items().size());
        assertEquals(3, response.items().get(0).quantity());
        assertEquals(9, product.getStockQuantity());

        assertEquals(
                0,
                new BigDecimal("9600.00")
                        .compareTo(response.totalAmount())
        );

        verify(productRepository, times(1))
                .findByIdForUpdate(2L);

        verify(orderRepository, times(1))
                .saveAndFlush(any(Order.class));

        verify(outboxEventRepository, times(1))
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectOrderWhenProductDoesNotExist() {
        mockCustomer();

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(99L, 1))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(
                        request,
                        CUSTOMER_EMAIL
                )
        );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .saveAndFlush(any(Order.class));

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldSaveOutboxEventWhenOrderIsCreated() {
        mockCustomer();
        mockSavedOrder();

        Product product = createProduct(12);

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(product));

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(2L, 1))
        );

        orderService.createOrder(
                request,
                CUSTOMER_EMAIL
        );

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();

        assertEquals(5L, savedEvent.getOrderId());
        assertEquals("5", savedEvent.getPayload());
        assertEquals(
                OutboxStatus.PENDING,
                savedEvent.getStatus()
        );
        assertEquals(0, savedEvent.getAttempts());
        assertNull(savedEvent.getPublishedAt());

        InOrder executionOrder = inOrder(
                orderRepository,
                outboxEventRepository
        );

        executionOrder.verify(orderRepository)
                .saveAndFlush(any(Order.class));

        executionOrder.verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    private void mockCustomer() {
        User customer = mock(User.class);

        when(userRepository.findByEmail(CUSTOMER_EMAIL))
                .thenReturn(Optional.of(customer));
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
                2L
        );

        return product;
    }

    private void mockSavedOrder() {
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            order,
                            "id",
                            5L
                    );

                    ReflectionTestUtils.setField(
                            order,
                            "createdAt",
                            LocalDateTime.of(
                                    2026,
                                    10,
                                    18,
                                    12,
                                    26
                            )
                    );

                    return order;
                });
    }
}
