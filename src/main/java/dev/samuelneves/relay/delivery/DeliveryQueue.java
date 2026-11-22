package dev.samuelneves.relay.delivery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * As duas operacoes de banco do worker, cada uma numa transacao curta.
 * Ficam num bean separado do worker de proposito: @Transactional funciona por proxy,
 * e uma chamada de um metodo para outro da mesma classe passaria por fora dele.
 */
@Service
public class DeliveryQueue {

    /** Quanto tempo uma entrega fica reservada. Bem maior que o timeout HTTP. */
    private static final Duration LEASE = Duration.ofMinutes(1);

    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;
    private final RetryPolicy retryPolicy;

    public DeliveryQueue(DeliveryRepository deliveries,
                         DeliveryAttemptRepository attempts,
                         RetryPolicy retryPolicy) {
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.retryPolicy = retryPolicy;
    }

    /** Reserva ate batchSize entregas vencidas e devolve o necessario para envia-las. */
    @Transactional
    public List<DeliveryJob> claimBatch(int batchSize) {
        Instant now = Instant.now();
        List<DeliveryJob> jobs = new ArrayList<>();

        for (Delivery delivery : deliveries.findDueForUpdate(now, batchSize)) {
            if (!delivery.getEndpoint().isActive()) {
                delivery.cancel();
                continue;
            }
            delivery.claim(now.plus(LEASE));
            jobs.add(DeliveryJob.of(delivery));
        }
        // Sem save explicito: o Hibernate grava as mudancas das entidades no commit (dirty checking)
        return jobs;
    }

    /** Registra a tentativa e decide o proximo passo: sucesso, novo retry ou desistir. */
    @Transactional
    public void recordResult(UUID deliveryId, SendResult result) {
        Delivery delivery = deliveries.findById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Entrega " + deliveryId + " sumiu"));

        attempts.save(DeliveryAttempt.of(delivery, result));

        int attemptNumber = delivery.getAttemptCount() + 1;
        if (result.success()) {
            delivery.succeed(result.statusCode());
        } else if (retryPolicy.hasRetryAfter(attemptNumber)) {
            Instant next = Instant.now().plus(retryPolicy.delayAfter(attemptNumber));
            delivery.retryAt(next, result.statusCode(), result.error());
        } else {
            delivery.giveUp(result.statusCode(), result.error());
        }
    }
}
