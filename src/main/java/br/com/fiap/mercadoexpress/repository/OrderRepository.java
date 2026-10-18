
package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_Id(Long customerId);

}
