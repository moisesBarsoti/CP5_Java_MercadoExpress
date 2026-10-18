
package br.com.fiap.mercadoexpress.repository;

import br.com.fiap.mercadoexpress.entity.EmailLog;
import br.com.fiap.mercadoexpress.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository
        extends JpaRepository<EmailLog, Long> {

    boolean existsByOrder_IdAndStatus(
            Long orderId,
            EmailStatus status
    );
}