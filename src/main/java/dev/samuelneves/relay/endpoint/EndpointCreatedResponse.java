package dev.samuelneves.relay.endpoint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta do cadastro: a unica vez em que o segredo aparece.
 * Quem cadastra precisa guarda-lo para verificar as assinaturas.
 */
public record EndpointCreatedResponse(
        UUID id,
        String url,
        List<String> eventTypes,
        boolean active,
        Instant createdAt,
        String secret
) {
    public static EndpointCreatedResponse from(Endpoint endpoint) {
        return new EndpointCreatedResponse(
                endpoint.getId(),
                endpoint.getUrl(),
                endpoint.getEventTypes(),
                endpoint.isActive(),
                endpoint.getCreatedAt(),
                endpoint.getSecret()
        );
    }
}
