package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.event.Event;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes das transicoes de estado da entrega, sem banco e sem Spring. */
class DeliveryTest {

    private Delivery novaEntrega() {
        Event event = new Event("pedido.criado", Map.of("pedidoId", 1));
        Endpoint endpoint = new Endpoint("https://exemplo.com/webhook", List.of("pedido.criado"));
        return new Delivery(event, endpoint);
    }

    @Test
    void comecaPendenteSemTentativas() {
        Delivery d = novaEntrega();

        assertEquals(DeliveryStatus.PENDING, d.getStatus());
        assertEquals(0, d.getAttemptCount());
    }

    @Test
    void cadaTentativaConcluidaIncrementaOContadorELiberaAReserva() {
        Delivery d = novaEntrega();
        d.claim(Instant.now().plusSeconds(60));
        assertEquals(DeliveryStatus.IN_PROGRESS, d.getStatus());

        Instant depois = Instant.now().plusSeconds(10);
        d.retryAt(depois, 500, null);

        assertEquals(DeliveryStatus.PENDING, d.getStatus());
        assertEquals(1, d.getAttemptCount());
        assertEquals(depois, d.getNextAttemptAt());
        assertEquals(500, d.getLastStatusCode());
        assertNull(d.getLockedUntil());
    }

    @Test
    void requeueReiniciaUmaEntregaMorta() {
        Delivery d = novaEntrega();
        d.claim(Instant.now().plusSeconds(60));
        d.giveUp(404, null);
        assertEquals(DeliveryStatus.DEAD, d.getStatus());

        d.requeue();

        assertEquals(DeliveryStatus.PENDING, d.getStatus());
        assertEquals(0, d.getAttemptCount());
        assertFalse(d.getNextAttemptAt().isAfter(Instant.now()));
    }

    @Test
    void naoReprocessaEntregaQueNaoEstaMorta() {
        Delivery pendente = novaEntrega();
        assertThrows(IllegalStateException.class, pendente::requeue);

        Delivery entregue = novaEntrega();
        entregue.claim(Instant.now().plusSeconds(60));
        entregue.succeed(200);
        assertThrows(IllegalStateException.class, entregue::requeue);
    }
}
