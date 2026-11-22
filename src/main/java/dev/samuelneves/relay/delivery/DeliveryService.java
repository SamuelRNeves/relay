package dev.samuelneves.relay.delivery;

import dev.samuelneves.relay.endpoint.Endpoint;
import dev.samuelneves.relay.event.Event;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Faz uma tentativa de entrega HTTP de um evento a um endpoint e registra o resultado. */
@Service
public class DeliveryService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final DeliveryAttemptRepository attempts;

    public DeliveryService(DeliveryAttemptRepository attempts) {
        this.attempts = attempts;

        // Sem timeout, um destino lento prenderia a thread indefinidamente
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public DeliveryAttempt deliver(Event event, Endpoint endpoint) {
        long start = System.nanoTime();
        DeliveryAttempt attempt;

        try {
            int status = restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Relay-Event-Id", event.getId().toString())
                    .header("X-Relay-Event-Type", event.getType())
                    .body(new WebhookMessage(event.getId(), event.getType(), event.getCreatedAt(), event.getPayload()))
                    // exchange devolve o status sem lancar excecao em 4xx/5xx:
                    // para nos, erro do destino e um resultado a registrar, nao uma falha do Relay
                    .exchange((request, response) -> response.getStatusCode().value());

            attempt = DeliveryAttempt.withResponse(event, endpoint, status, elapsedMs(start));
        } catch (RestClientException e) {
            // Sem resposta HTTP: timeout, DNS, conexao recusada...
            attempt = DeliveryAttempt.withError(event, endpoint, describe(e), elapsedMs(start));
        }

        return attempts.save(attempt);
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private static String describe(Exception e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    /** O corpo JSON que o destino recebe. */
    record WebhookMessage(UUID id, String type, Instant createdAt, Map<String, Object> payload) {
    }
}
