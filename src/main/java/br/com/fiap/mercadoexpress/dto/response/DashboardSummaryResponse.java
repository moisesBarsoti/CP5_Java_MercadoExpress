
package br.com.fiap.mercadoexpress.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalOrders,
        long paidOrders,
        long pendingOrders,
        long failedOrders,
        BigDecimal totalRevenue,
        long totalProducts,
        long availableStock
) {}