
package br.com.fiap.mercadoexpress.dto.bank;

public record BankPaymentResponse(
        String transactionId,
        Long orderId,
        String status,
        String message
) {
}
