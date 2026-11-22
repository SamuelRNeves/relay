package dev.samuelneves.relay.delivery;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class DeliveryController {

    private final DeliveryAdminService service;

    public DeliveryController(DeliveryAdminService service) {
        this.service = service;
    }

    /** Ex.: GET /deliveries?status=DEAD&endpointId=...&page=0&size=20 */
    @GetMapping("/deliveries")
    public List<DeliveryResponse> list(
            @RequestParam(defaultValue = "DEAD") DeliveryStatus status,
            @RequestParam(required = false) UUID endpointId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, endpointId, page, size);
    }

    @PostMapping("/deliveries/{id}/retry")
    public DeliveryResponse retry(@PathVariable UUID id) {
        return service.retry(id);
    }

    @PostMapping("/endpoints/{id}/replay")
    public ReplayResponse replay(@PathVariable UUID id) {
        return new ReplayResponse(service.replayEndpoint(id));
    }

    public record ReplayResponse(int requeued) {
    }
}
