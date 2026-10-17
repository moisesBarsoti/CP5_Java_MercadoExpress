
package br.com.fiap.mercadoexpress.controller;

import br.com.fiap.mercadoexpress.dto.request.UserRequest;
import br.com.fiap.mercadoexpress.dto.response.UserResponse;
import br.com.fiap.mercadoexpress.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@Valid @RequestBody UserRequest request) {
        return authService.registerUser(request);
    }
}
