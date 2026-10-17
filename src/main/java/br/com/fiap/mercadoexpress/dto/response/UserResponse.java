package br.com.fiap.mercadoexpress.dto.response;

import br.com.fiap.mercadoexpress.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
}