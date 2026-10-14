package br.com.fiap.mercadoexpress.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank
    String name,

    @NotNull
    @DecimalMin("0.01")
    BigDecimal price,

    @NotNull
    @Min(0)
    Integer stockQuantity
) {}
