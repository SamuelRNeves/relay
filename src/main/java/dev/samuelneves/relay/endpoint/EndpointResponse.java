package dev.samuelneves.relay.endpoint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** O que a API devolve sobre um endpoint (nunca a entidade JPA direto). */
public record EndpointResponse(
        UUID id,
        String url,
        List<String> eventTypes,
        boolean active,
        Instant createdAt
) {
    public static EndpointResponse from(Endpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getUrl(),
                endpoint.getEventTypes(),
                endpoint.isActive(),
                endpoint.getCreatedAt()
        );
    }
}
