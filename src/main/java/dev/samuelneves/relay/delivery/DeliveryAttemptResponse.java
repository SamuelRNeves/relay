package dev.samuelneves.relay.delivery;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptResponse(
        UUID id,
        UUID endpointId,
        boolean success,
        Integer statusCode,
        String errorMessage,
        long durationMs,
        Instant attemptedAt
) {
    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return new DeliveryAttemptResponse(
                attempt.getId(),
                // Ler so o id de uma associacao LAZY nao dispara consulta ao banco
                attempt.getEndpoint().getId(),
                attempt.isSuccess(),
                attempt.getStatusCode(),
                attempt.getErrorMessage(),
                attempt.getDurationMs(),
                attempt.getAttemptedAt()
        );
    }
}
