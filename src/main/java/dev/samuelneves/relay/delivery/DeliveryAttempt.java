package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.event.Event;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Historico imutavel: uma linha por chamada HTTP feita a um destino. */
@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nulo apenas nas tentativas feitas antes da fila existir (etapa 1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id")
    private Endpoint endpoint;

    @Column(nullable = false)
    private boolean success;

    // Nulo quando nem chegou resposta (timeout, DNS, conexao recusada)
    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    protected DeliveryAttempt() {
        // exigido pelo JPA
    }

    private DeliveryAttempt(Delivery delivery, SendResult result) {
        this.delivery = delivery;
        this.event = delivery.getEvent();
        this.endpoint = delivery.getEndpoint();
        this.success = result.success();
        this.statusCode = result.statusCode();
        this.errorMessage = result.error();
        this.durationMs = result.durationMs();
        this.attemptedAt = Instant.now();
    }

    static DeliveryAttempt of(Delivery delivery, SendResult result) {
        return new DeliveryAttempt(delivery, result);
    }

    public UUID getId() { return id; }
    public Delivery getDelivery() { return delivery; }
    public Event getEvent() { return event; }
    public Endpoint getEndpoint() { return endpoint; }
    public boolean isSuccess() { return success; }
    public Integer getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
