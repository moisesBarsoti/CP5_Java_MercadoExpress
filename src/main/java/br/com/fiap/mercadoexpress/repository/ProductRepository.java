package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
