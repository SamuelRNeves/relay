package dev.samuelneves.relay.delivery;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Tudo que o worker precisa para enviar uma entrega, copiado das entidades
 * ainda dentro da transacao. Depois dela, nada de associacao LAZY para carregar.
 */
public record DeliveryJob(
        UUID deliveryId,
        UUID eventId,
        String eventType,
        Instant eventCreatedAt,
        Map<String, Object> payload,
        String endpointUrl
) {
    static DeliveryJob of(Delivery delivery) {
        var event = delivery.getEvent();
        return new DeliveryJob(
                delivery.getId(),
                event.getId(),
                event.getType(),
                event.getCreatedAt(),
                event.getPayload(),
                delivery.getEndpoint().getUrl()
        );
    }
}
