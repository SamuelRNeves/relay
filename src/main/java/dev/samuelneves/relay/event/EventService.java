package dev.samuelneves.relay.event;

import dev.samuelneves.relay.delivery.Delivery;
import dev.samuelneves.relay.delivery.DeliveryAttemptRepository;
import dev.samuelneves.relay.delivery.DeliveryAttemptResponse;
import dev.samuelneves.relay.delivery.DeliveryRepository;
import dev.samuelneves.relay.delivery.DeliveryResponse;
import dev.samuelneves.relay.endpoint.EndpointRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository events;
    private final EndpointRepository endpoints;
    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;

    public EventService(EventRepository events,
                        EndpointRepository endpoints,
                        DeliveryRepository deliveries,
                        DeliveryAttemptRepository attempts) {
        this.events = events;
        this.endpoints = endpoints;
        this.deliveries = deliveries;
        this.attempts = attempts;
    }

    /**
     * So enfileira: salva o evento e uma entrega PENDING por endpoint inscrito.
     * Tudo numa transacao, entao nunca existe evento sem as entregas, nem o contrario.
     * Aqui @Transactional e seguro porque nao ha nenhuma chamada HTTP: o worker cuida disso.
     */
    @Transactional
    public EventResponse publish(PublishEventRequest request) {
        Event event = events.save(new Event(request.type(), request.payload()));

        List<DeliveryResponse> queued = endpoints.findActiveSubscribedTo(event.getType()).stream()
                .map(endpoint -> deliveries.save(new Delivery(event, endpoint)))
                .map(DeliveryResponse::from)
                .toList();

        return EventResponse.from(event, queued);
    }

    @Transactional(readOnly = true)
    public EventResponse find(UUID id) {
        Event event = events.findById(id).orElseThrow(() -> notFound(id));
        List<DeliveryResponse> current = deliveries.findByEvent_IdOrderByCreatedAtAsc(id).stream()
                .map(DeliveryResponse::from)
                .toList();
        return EventResponse.from(event, current);
    }

    @Transactional(readOnly = true)
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
