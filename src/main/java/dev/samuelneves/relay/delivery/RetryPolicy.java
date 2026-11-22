package dev.samuelneves.relay.delivery;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Quando tentar de novo depois de uma falha.
 *
 * Os intervalos crescem (backoff exponencial) para dar tempo de um destino fora do ar
 * se recuperar sem ser bombardeado. O jitter espalha os retries em +/-20%: sem ele,
 * milhares de entregas que falharam juntas voltariam todas no mesmo segundo.
 *
 * Classe Java pura, sem Spring: e testada isoladamente.
 */
public class RetryPolicy {

    private static final double JITTER = 0.2;

    private final List<Duration> delays;
    private final DoubleSupplier random;

    public RetryPolicy(List<Duration> delays) {
        this(delays, () -> ThreadLocalRandom.current().nextDouble());
    }

    /** O gerador aleatorio e injetavel para os testes serem deterministicos. */
    RetryPolicy(List<Duration> delays, DoubleSupplier random) {
        if (delays.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um intervalo de retry");
        }
        this.delays = List.copyOf(delays);
        this.random = random;
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(List.of(
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofMinutes(2),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30)
        ));
    }

    /** Total de tentativas: a primeira mais um retry para cada intervalo. */
    public int maxAttempts() {
        return delays.size() + 1;
    }

    /** Depois da tentativa de numero attemptNumber (1, 2, 3...) ainda ha outra? */
    public boolean hasRetryAfter(int attemptNumber) {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber comeca em 1");
        }
        return attemptNumber < maxAttempts();
    }

    /** Quanto esperar depois da tentativa attemptNumber, ja com jitter. */
    public Duration delayAfter(int attemptNumber) {
        if (!hasRetryAfter(attemptNumber)) {
            throw new IllegalArgumentException("Nao ha retry depois da tentativa " + attemptNumber);
        }
        Duration base = delays.get(attemptNumber - 1);
        // random() em [0, 1) vira um fator em [0.8, 1.2)
        double factor = 1 - JITTER + (2 * JITTER * random.getAsDouble());
        return Duration.ofMillis(Math.round(base.toMillis() * factor));
    }
}
