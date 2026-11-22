package dev.samuelneves.relay.delivery;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    private static final List<Duration> DELAYS = List.of(
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2)
    );

    @Test
    void permiteUmaTentativaAMaisQueOsIntervalos() {
        RetryPolicy policy = new RetryPolicy(DELAYS, () -> 0.5);

        assertEquals(4, policy.maxAttempts());
        assertTrue(policy.hasRetryAfter(1));
        assertTrue(policy.hasRetryAfter(3));
        assertFalse(policy.hasRetryAfter(4), "a quarta tentativa e a ultima");
    }

    @Test
    void semJitterOIntervaloEOConfigurado() {
        // 0.5 e o meio da faixa: fator exatamente 1.0
        RetryPolicy policy = new RetryPolicy(DELAYS, () -> 0.5);

        assertEquals(Duration.ofSeconds(10), policy.delayAfter(1));
        assertEquals(Duration.ofSeconds(30), policy.delayAfter(2));
        assertEquals(Duration.ofMinutes(2), policy.delayAfter(3));
    }

    @Test
    void jitterFicaEntreMenosEMaisVintePorCento() {
        RetryPolicy menor = new RetryPolicy(DELAYS, () -> 0.0);
        RetryPolicy maior = new RetryPolicy(DELAYS, () -> 0.999999);

        assertEquals(Duration.ofSeconds(8), menor.delayAfter(1));
        Duration quaseMaximo = maior.delayAfter(1);
        assertTrue(quaseMaximo.compareTo(Duration.ofSeconds(12)) <= 0);
        assertTrue(quaseMaximo.compareTo(Duration.ofMillis(11_999)) >= 0);
    }

    @Test
    void naoHaIntervaloDepoisDaUltimaTentativa() {
        RetryPolicy policy = new RetryPolicy(DELAYS, () -> 0.5);

        assertThrows(IllegalArgumentException.class, () -> policy.delayAfter(4));
    }

    @Test
    void numeroDeTentativaComecaEmUm() {
        RetryPolicy policy = new RetryPolicy(DELAYS, () -> 0.5);

        assertThrows(IllegalArgumentException.class, () -> policy.hasRetryAfter(0));
    }

    @Test
    void exigeAoMenosUmIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(List.of(), () -> 0.5));
    }
}
