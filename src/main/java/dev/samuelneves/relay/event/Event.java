package dev.samuelneves.relay.event;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String type;

    // Guardado como JSONB; o Hibernate converte o Map para JSON e de volta
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // exigido pelo JPA
    }

    public Event(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = new LinkedHashMap<>(payload); // aceita valores null do JSON, ao contrario de Map.copyOf
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
