package dev.samuelneves.relay.delivery;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Faz a chamada HTTP de uma entrega. Nao toca no banco: so envia e mede. */
@Component
public class WebhookSender {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public WebhookSender() {
        // Sem timeout, um destino lento prenderia o worker indefinidamente
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public SendResult send(DeliveryJob job) {
        long start = System.nanoTime();
        try {
            int status = restClient.post()
                    .uri(job.endpointUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    // O destino usa este id para ignorar entregas repetidas (idempotencia)
                    .header("X-Relay-Event-Id", job.eventId().toString())
                    .header("X-Relay-Event-Type", job.eventType())
                    .body(new WebhookMessage(job.eventId(), job.eventType(), job.eventCreatedAt(), job.payload()))
                    // exchange devolve o status sem lancar excecao em 4xx/5xx:
                    // erro do destino e um resultado a registrar, nao uma falha do Relay
                    .exchange((request, response) -> response.getStatusCode().value());

            return SendResult.response(status, elapsedMs(start));
        } catch (RestClientException e) {
            // Sem resposta HTTP: timeout, DNS, conexao recusada...
            return SendResult.failure(describe(e), elapsedMs(start));
        }
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
