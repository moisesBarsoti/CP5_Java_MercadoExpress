
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.response.DashboardSummaryResponse;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        BigDecimal revenue = Optional.ofNullable(
                orderRepository.sumTotalByStatus(OrderStatus.PAID)
        ).orElse(BigDecimal.ZERO);

        Long availableStock = productRepository.sumAvailableStock();

        return new DashboardSummaryResponse(
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PAID),
                orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT),
                orderRepository.countByStatus(OrderStatus.PAYMENT_FAILED),
                revenue,
                productRepository.count(),
                availableStock
        );
    }
}