package dev.samuelneves.relay.event;

import dev.samuelneves.relay.delivery.DeliveryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        Instant createdAt,
        List<DeliveryResponse> deliveries
) {
    public static EventResponse from(Event event, List<DeliveryResponse> deliveries) {
        return new EventResponse(event.getId(), event.getType(), event.getPayload(), event.getCreatedAt(), deliveries);
    }
}
