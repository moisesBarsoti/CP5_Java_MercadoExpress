package br.com.fiap.mercadoexpress.dto.bank;

import java.math.BigDecimal;

public record BankPaymentRequest(
        Long orderId,
        BigDecimal amount
) {}