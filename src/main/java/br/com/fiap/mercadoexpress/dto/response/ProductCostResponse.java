
package br.com.fiap.mercadoexpress.dto.response;

import java.math.BigDecimal;

public record ProductCostResponse(
        BigDecimal costPrice
) {}