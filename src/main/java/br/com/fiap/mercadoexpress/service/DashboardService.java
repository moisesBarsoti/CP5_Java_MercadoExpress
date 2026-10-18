
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.response.DashboardSummaryResponse;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import br.com.fiap.mercadoexpress.repository.OrderRepository;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.mercadoexpress.dto.response.DailySalesResponse;
import br.com.fiap.mercadoexpress.dto.response.ProductStockResponse;
import br.com.fiap.mercadoexpress.dto.response.DashboardChartsResponse;
import br.com.fiap.mercadoexpress.entity.Order;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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


    @Transactional(readOnly = true)
    public DashboardChartsResponse getCharts() {

        Map<LocalDate, List<Order>> ordersByDate =
                orderRepository.findAllByStatus(OrderStatus.PAID)
                        .stream()
                        .collect(Collectors.groupingBy(
                                order -> order.getCreatedAt().toLocalDate(),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        List<DailySalesResponse> salesByDay =
                ordersByDate.entrySet()
                        .stream()
                        .map(entry -> new DailySalesResponse(
                                entry.getKey(),
                                entry.getValue().size(),
                                entry.getValue()
                                        .stream()
                                        .map(Order::getTotalAmount)
                                        .reduce(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )
                        ))
                        .toList();

        List<ProductStockResponse> stockByProduct =
                productRepository.findAll(Sort.by("name"))
                        .stream()
                        .map(product -> new ProductStockResponse(
                                product.getId(),
                                product.getName(),
                                product.getStockQuantity()
                        ))
                        .toList();

        return new DashboardChartsResponse(
                salesByDay,
                stockByProduct
        );
    }

}