
package br.com.fiap.mercadoexpress.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardProfitResponse(
        BigDecimal recordedGrossProfit,
        long fullyCostedOrders,
        long ordersMissingCost,
        List<DailyProfitResponse> profitByDay
) {}