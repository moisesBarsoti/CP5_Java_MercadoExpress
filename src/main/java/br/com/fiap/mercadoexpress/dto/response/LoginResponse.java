
package br.com.fiap.mercadoexpress.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn
) {
}