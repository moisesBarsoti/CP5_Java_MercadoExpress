
package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.Order;
import br.com.fiap.mercadoexpress.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_Id(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(OrderStatus status);

    @Query("""
        SELECT SUM(o.totalAmount)
        FROM Order o
        WHERE o.status = :status
        """)
    BigDecimal sumTotalByStatus(
            @Param("status") OrderStatus status
    );

    List<Order> findAllByStatus(OrderStatus status);
}
