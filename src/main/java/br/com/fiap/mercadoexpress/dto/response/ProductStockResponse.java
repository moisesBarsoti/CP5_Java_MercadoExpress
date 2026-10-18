
package br.com.fiap.mercadoexpress.dto.response;

public record ProductStockResponse(
        Long productId,
        String productName,
        Integer availableQuantity
) {}