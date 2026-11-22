package dev.samuelneves.relay.event;

import dev.samuelneves.relay.delivery.DeliveryAttempt;
import dev.samuelneves.relay.delivery.DeliveryAttemptRepository;
import dev.samuelneves.relay.delivery.DeliveryAttemptResponse;
import dev.samuelneves.relay.delivery.DeliveryService;
import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.endpoint.EndpointRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository events;
    private final EndpointRepository endpoints;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryService deliveryService;

    public EventService(EventRepository events,
                        EndpointRepository endpoints,
                        DeliveryAttemptRepository attempts,
                        DeliveryService deliveryService) {
        this.events = events;
        this.endpoints = endpoints;
        this.attempts = attempts;
        this.deliveryService = deliveryService;
    }

    /**
     * Etapa 1: entrega sincrona, dentro da propria requisicao.
     * De proposito NAO e @Transactional: manter uma transacao aberta durante chamadas HTTP
     * prenderia uma conexao do banco enquanto esperamos destinos possivelmente lentos.
     * Cada save abaixo e uma transacao curta e independente.
     */
    public EventResponse publish(PublishEventRequest request) {
        Event event = events.save(new Event(request.type(), request.payload()));

        List<DeliveryAttemptResponse> results = new ArrayList<>();
        for (Endpoint endpoint : endpoints.findActiveSubscribedTo(event.getType())) {
            DeliveryAttempt attempt = deliveryService.deliver(event, endpoint);
            results.add(DeliveryAttemptResponse.from(attempt));
        }

        return EventResponse.from(event, results);
    }

    public EventResponse find(UUID id) {
        Event event = events.findById(id).orElseThrow(() -> notFound(id));
        return EventResponse.from(event, attemptsOf(id));
    }

    public List<DeliveryAttemptResponse> attemptsOf(UUID eventId) {
        if (!events.existsById(eventId)) {
            throw notFound(eventId);
        }
        return attempts.findByEvent_IdOrderByAttemptedAtAsc(eventId).stream()
                .map(DeliveryAttemptResponse::from)
                .toList();
    }

    private static ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento " + id + " não encontrado.");
    }
}
