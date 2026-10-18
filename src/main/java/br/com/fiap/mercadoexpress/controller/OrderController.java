
package br.com.fiap.mercadoexpress.controller;

import br.com.fiap.mercadoexpress.dto.request.OrderRequest;
import br.com.fiap.mercadoexpress.dto.response.OrderResponse;
import br.com.fiap.mercadoexpress.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        OrderResponse response = orderService.createOrder(
                request,
                jwt.getSubject()
        );

        URI location = URI.create("/api/orders/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }
}
