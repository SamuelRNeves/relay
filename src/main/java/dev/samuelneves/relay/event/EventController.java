package dev.samuelneves.relay.event;

import dev.samuelneves.relay.delivery.DeliveryAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EventResponse> publish(@Valid @RequestBody PublishEventRequest request) {
        EventResponse response = service.publish(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        // 202 Accepted: o evento foi aceito, mas a entrega acontece depois, pelo worker
        return ResponseEntity.accepted().location(location).body(response);
    }

    @GetMapping("/{id}")
    public EventResponse find(@PathVariable UUID id) {
        return service.find(id);
    }

    @GetMapping("/{id}/attempts")
    public List<DeliveryAttemptResponse> attempts(@PathVariable UUID id) {
        return service.attemptsOf(id);
    }
}
