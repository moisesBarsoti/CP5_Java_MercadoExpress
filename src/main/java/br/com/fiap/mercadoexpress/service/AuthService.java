
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.request.LoginRequest;
import br.com.fiap.mercadoexpress.dto.request.UserRequest;
import br.com.fiap.mercadoexpress.dto.response.LoginResponse;
import br.com.fiap.mercadoexpress.dto.response.UserResponse;
import br.com.fiap.mercadoexpress.entity.User;
import br.com.fiap.mercadoexpress.enums.Role;
import br.com.fiap.mercadoexpress.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse registerUser(UserRequest request) {
        boolean existsEmail = userRepository.existsByEmail(request.email());

        if (existsEmail) {
            throw new IllegalArgumentException("Email already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(encodedPassword)
                .role(Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public User authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
                );

        boolean validPassword = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!validPassword) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return user;
    }

    public LoginResponse loginUser(LoginRequest request) {
        User user = authenticateUser(request);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}
