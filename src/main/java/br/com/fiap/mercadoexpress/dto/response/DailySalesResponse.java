
package br.com.fiap.mercadoexpress.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesResponse(
        LocalDate date,
        long orders,
        BigDecimal revenue
) {}