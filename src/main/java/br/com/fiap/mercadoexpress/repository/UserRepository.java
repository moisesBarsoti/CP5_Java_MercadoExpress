package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
