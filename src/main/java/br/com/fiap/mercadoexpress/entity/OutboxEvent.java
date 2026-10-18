
package br.com.fiap.mercadoexpress.entity;

import br.com.fiap.mercadoexpress.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_status_created",
                        columnList = "status, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_id",
            nullable = false,
            unique = true
    )
    private Long orderId;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    private Long version;

    private OutboxEvent(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid order ID"
            );
        }

        this.orderId = orderId;
        this.payload = orderId.toString();
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static OutboxEvent paymentRequested(Long orderId) {
        return new OutboxEvent(orderId);
    }

    public void recordFailure() {

        if (status != OutboxStatus.PENDING) {
            throw new IllegalStateException(
                    "Event is already published"
            );
        }

        attempts++;
    }

    public void markAsPublished() {

        if (status != OutboxStatus.PENDING) {
            throw new IllegalStateException(
                    "Event is already published"
            );
        }

        status = OutboxStatus.PUBLISHED;
        publishedAt = LocalDateTime.now();
    }
}