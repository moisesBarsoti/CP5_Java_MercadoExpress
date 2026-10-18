
package br.com.fiap.mercadoexpress.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyProfitResponse(
        LocalDate date,
        BigDecimal recordedGrossProfit,
        long fullyCostedOrders,
        long ordersMissingCost
) {}