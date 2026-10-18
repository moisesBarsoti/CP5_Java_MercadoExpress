
package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.OutboxEvent;
import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop20ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );

    Optional<OutboxEvent> findByOrderId(Long orderId);
}
