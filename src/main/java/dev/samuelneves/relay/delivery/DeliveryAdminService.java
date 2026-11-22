package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.endpoint.EndpointRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Operacoes manuais sobre a fila: consultar e reprocessar entregas. */
@Service
public class DeliveryAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DeliveryRepository deliveries;
    private final EndpointRepository endpoints;

    public DeliveryAdminService(DeliveryRepository deliveries, EndpointRepository endpoints) {
        this.deliveries = deliveries;
        this.endpoints = endpoints;
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> list(DeliveryStatus status, UUID endpointId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page deve ser >= 0 e size entre 1 e " + MAX_PAGE_SIZE + ".");
        }
        Pageable pageable = PageRequest.of(page, size);
        List<Delivery> result = endpointId == null
                ? deliveries.findByStatusOrderByUpdatedAtDesc(status, pageable)
                : deliveries.findByStatusAndEndpoint_IdOrderByUpdatedAtDesc(status, endpointId, pageable);
        return result.stream().map(DeliveryResponse::from).toList();
    }

    /** Devolve uma entrega morta para a fila. */
    @Transactional
    public DeliveryResponse retry(UUID deliveryId) {
        Delivery delivery = deliveries.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrega " + deliveryId + " não encontrada."));

        if (!delivery.getEndpoint().isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O endpoint desta entrega está desativado.");
        }
        try {
            delivery.requeue();
        } catch (IllegalStateException e) {
            // 409 Conflict: a requisicao e valida, mas o estado atual do recurso nao permite
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só entregas DEAD podem ser reprocessadas; esta está " + delivery.getStatus() + ".");
        }
        return DeliveryResponse.from(delivery);
    }

    /** Devolve para a fila todas as entregas mortas de um endpoint. Devolve quantas foram. */
    @Transactional
    public int replayEndpoint(UUID endpointId) {
        Endpoint endpoint = endpoints.findById(endpointId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Endpoint " + endpointId + " não encontrado."));
        if (!endpoint.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O endpoint está desativado.");
        }
        return deliveries.requeueDeadByEndpoint(endpointId, Instant.now());
    }
}
