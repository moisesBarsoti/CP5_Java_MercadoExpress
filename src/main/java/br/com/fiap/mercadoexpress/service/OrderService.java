
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.request.OrderItemRequest;
import br.com.fiap.mercadoexpress.dto.request.OrderRequest;
import br.com.fiap.mercadoexpress.dto.response.OrderItemResponse;
import br.com.fiap.mercadoexpress.dto.response.OrderResponse;
import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.entity.User;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import br.com.fiap.mercadoexpress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String email) {

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (request == null || request.items() == null
                || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain items");
        }

        Map<Long, Integer> quantities = new TreeMap<>();

        for (OrderItemRequest item : request.items()) {

            if (item == null || item.productId() == null
                    || item.productId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {
                throw new IllegalArgumentException("Invalid order item");
            }

            quantities.merge(
                    item.productId(),
                    item.quantity(),
                    Math::addExact
            );
        }

        Order order = Order.builder()
                .customer(customer)
                .build();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {

            Product product = productRepository
                    .findByIdForUpdate(entry.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Product not found")
                    );

            int quantity = entry.getValue();

            product.decreaseStock(quantity);
            order.addItem(product, quantity);
        }

        Order savedOrder = orderRepository.saveAndFlush(order);

        return toResponse(savedOrder);
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),

                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        ))
                        .toList()
        );
    }
}
