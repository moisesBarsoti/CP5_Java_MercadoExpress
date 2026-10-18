package br.com.fiap.mercadoexpress.dto.request;

import java.math.BigDecimal;

public record BankPaymentRequest(
        Long orderId,
        BigDecimal amount
) {}