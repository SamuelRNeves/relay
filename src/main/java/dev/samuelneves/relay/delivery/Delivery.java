package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.event.Event;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Uma entrega pendente ou concluida de um evento para um endpoint: a "mensagem" da fila. */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id")
    private Endpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Delivery() {
        // exigido pelo JPA
    }

    /** Cria uma entrega pronta para a primeira tentativa. */
    public Delivery(Event event, Endpoint endpoint) {
        Instant now = Instant.now();
        this.event = event;
        this.endpoint = endpoint;
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Reserva a entrega para um worker. Se ele morrer, outro pode pegar depois de lockedUntil. */
    void claim(Instant lockedUntil) {
        this.status = DeliveryStatus.IN_PROGRESS;
        this.lockedUntil = lockedUntil;
        touch();
    }

    void succeed(int statusCode) {
        finishAttempt(DeliveryStatus.SUCCEEDED, statusCode, null);
    }

    void retryAt(Instant when, Integer statusCode, String error) {
        finishAttempt(DeliveryStatus.PENDING, statusCode, error);
        this.nextAttemptAt = when;
    }

    void giveUp(Integer statusCode, String error) {
        finishAttempt(DeliveryStatus.DEAD, statusCode, error);
    }

    void cancel() {
        this.status = DeliveryStatus.CANCELLED;
        this.lockedUntil = null;
        touch();
    }

    private void finishAttempt(DeliveryStatus newStatus, Integer statusCode, String error) {
        this.status = newStatus;
        this.attemptCount++;
        this.lastStatusCode = statusCode;
        this.lastError = error;
        this.lockedUntil = null;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public Endpoint getEndpoint() { return endpoint; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Integer getLastStatusCode() { return lastStatusCode; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
