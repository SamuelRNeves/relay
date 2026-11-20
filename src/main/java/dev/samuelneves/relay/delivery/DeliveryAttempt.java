package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.event.Event;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    private DeliveryAttempt(Event event, Endpoint endpoint, boolean success,
                            Integer statusCode, String errorMessage, long durationMs) {
        this.event = event;
        this.endpoint = endpoint;
        this.success = success;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.attemptedAt = Instant.now();
    }

    /** O destino respondeu com algum status HTTP (2xx conta como sucesso). */
    public static DeliveryAttempt withResponse(Event event, Endpoint endpoint, int statusCode, long durationMs) {
        boolean ok = statusCode >= 200 && statusCode < 300;
        return new DeliveryAttempt(event, endpoint, ok, statusCode, null, durationMs);
    }

    /** Nao houve resposta HTTP: timeout, erro de rede etc. */
    public static DeliveryAttempt withError(Event event, Endpoint endpoint, String errorMessage, long durationMs) {
        return new DeliveryAttempt(event, endpoint, false, null, errorMessage, durationMs);
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public Endpoint getEndpoint() { return endpoint; }
    public boolean isSuccess() { return success; }
    public Integer getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
