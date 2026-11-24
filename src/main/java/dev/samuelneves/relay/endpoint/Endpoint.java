package dev.samuelneves.relay.endpoint;

import dev.samuelneves.relay.signing.WebhookSignature;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String url;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "event_types", nullable = false)
    private List<String> eventTypes = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Usado para assinar as entregas. Nunca sai na API, exceto uma vez, no cadastro
    @Column(nullable = false, length = 100)
    private String secret;

    protected Endpoint() {
        // exigido pelo JPA
    }

    public Endpoint(String url, List<String> eventTypes) {
        this.url = url;
        this.eventTypes = new ArrayList<>(eventTypes);
        this.createdAt = Instant.now();
        this.secret = WebhookSignature.newSecret();
    }

    public boolean isSubscribedTo(String eventType) {
        return active && eventTypes.contains(eventType);
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() { return id; }
    public String getUrl() { return url; }
    public List<String> getEventTypes() { return List.copyOf(eventTypes); }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSecret() { return secret; }
}
