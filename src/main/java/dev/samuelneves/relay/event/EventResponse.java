package dev.samuelneves.relay.event;

import dev.samuelneves.relay.delivery.DeliveryAttemptResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        Instant createdAt,
        List<DeliveryAttemptResponse> attempts
) {
    public static EventResponse from(Event event, List<DeliveryAttemptResponse> attempts) {
        return new EventResponse(event.getId(), event.getType(), event.getPayload(), event.getCreatedAt(), attempts);
    }
}
