package dev.samuelneves.relay.endpoint;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/endpoints")
public class EndpointController {

    private final EndpointRepository repository;

    public EndpointController(EndpointRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<EndpointCreatedResponse> create(@Valid @RequestBody CreateEndpointRequest request) {
        Endpoint saved = repository.save(new Endpoint(request.url(), request.eventTypes()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(EndpointCreatedResponse.from(saved));
    }

    @GetMapping
    public List<EndpointResponse> list() {
        return repository.findAll().stream()
                .map(EndpointResponse::from)
                .toList();
    }

    /**
     * Desativa o endpoint em vez de apagar a linha: as tentativas de entrega
     * antigas continuam apontando para ele, e o historico fica preservado.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        Endpoint endpoint = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Endpoint " + id + " não encontrado."));
        endpoint.deactivate();
        repository.save(endpoint);
    }
}
