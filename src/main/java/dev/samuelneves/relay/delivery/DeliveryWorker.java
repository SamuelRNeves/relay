package dev.samuelneves.relay.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consome a fila: a cada intervalo reserva um lote, envia cada entrega e registra o resultado.
 * O envio HTTP fica FORA de transacao, para nao segurar conexao do banco esperando o destino.
 */
@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);
    private static final int BATCH_SIZE = 10;

    private final DeliveryQueue queue;
    private final WebhookSender sender;

    public DeliveryWorker(DeliveryQueue queue, WebhookSender sender) {
        this.queue = queue;
        this.sender = sender;
    }

    // fixedDelay: o proximo ciclo so comeca depois que este terminar, entao nunca ha dois ao mesmo tempo
    @Scheduled(fixedDelayString = "${relay.worker.poll-interval-ms:1000}")
    public void poll() {
        List<DeliveryJob> jobs = queue.claimBatch(BATCH_SIZE);

        for (DeliveryJob job : jobs) {
            try {
                SendResult result = sender.send(job);
                queue.recordResult(job.deliveryId(), result);
                log.info("Entrega {} para {}: status={} erro={} ({} ms)",
                        job.deliveryId(), job.endpointUrl(), result.statusCode(), result.error(), result.durationMs());
            } catch (RuntimeException e) {
                // Uma entrega com problema nao derruba o lote. Se nao foi registrada,
                // o lease expira e ela e tentada de novo.
                log.error("Falha ao processar a entrega {}", job.deliveryId(), e);
            }
        }
    }
}
