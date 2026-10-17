
package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.request.UserRequest;
import br.com.fiap.mercadoexpress.dto.response.UserResponse;
import br.com.fiap.mercadoexpress.entity.User;
import br.com.fiap.mercadoexpress.enums.Role;
import br.com.fiap.mercadoexpress.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
