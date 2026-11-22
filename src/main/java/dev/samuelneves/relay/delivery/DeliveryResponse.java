package dev.samuelneves.relay.delivery;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID endpointId,
        DeliveryStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Integer lastStatusCode,
        String lastError
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getEndpoint().getId(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getNextAttemptAt(),
                delivery.getLastStatusCode(),
                delivery.getLastError()
        );
    }
}
