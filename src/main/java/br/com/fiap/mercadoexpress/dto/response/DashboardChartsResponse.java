
package br.com.fiap.mercadoexpress.dto.response;

import java.util.List;

public record DashboardChartsResponse(
    List<DailySalesResponse> salesByDay,
    List<ProductStockResponse> stockByProduct
) {}