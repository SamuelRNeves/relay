package dev.samuelneves.relay.signing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureTest {

    private static final String SECRET = "whsec_teste";
    private static final byte[] BODY = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
    private static final long TIMESTAMP = 1_700_000_000L;
    private static final Instant AGORA = Instant.ofEpochSecond(TIMESTAMP);
    private static final Duration TOLERANCIA = Duration.ofMinutes(5);

    @Test
    void geraOHmacEsperado() {
        // Valor calculado de forma independente (Python: hmac.new(secret, b"1700000000.{\"a\":1}", sha256))
        assertEquals(
                "9331ca93856adeb864738a2e8bab0271c8c0c56b94b3510f128d2220bd79713b",
                WebhookSignature.sign(SECRET, TIMESTAMP, BODY));
    }

    @Test
    void headerTemTimestampEAssinatura() {
        assertEquals(
                "t=1700000000,v1=9331ca93856adeb864738a2e8bab0271c8c0c56b94b3510f128d2220bd79713b",
                WebhookSignature.header(SECRET, TIMESTAMP, BODY));
    }

    @Test
    void aceitaAssinaturaValida() {
        String header = WebhookSignature.header(SECRET, TIMESTAMP, BODY);

        assertTrue(WebhookSignature.verify(SECRET, header, BODY, AGORA, TOLERANCIA));
    }

    @Test
    void rejeitaCorpoAlterado() {
        String header = WebhookSignature.header(SECRET, TIMESTAMP, BODY);
        byte[] adulterado = "{\"a\":2}".getBytes(StandardCharsets.UTF_8);

        assertFalse(WebhookSignature.verify(SECRET, header, adulterado, AGORA, TOLERANCIA));
    }

    @Test
    void rejeitaSegredoErrado() {
        String header = WebhookSignature.header(SECRET, TIMESTAMP, BODY);

        assertFalse(WebhookSignature.verify("whsec_outro", header, BODY, AGORA, TOLERANCIA));
    }

    @Test
    void rejeitaTimestampAntigoMesmoComAssinaturaValida() {
        String header = WebhookSignature.header(SECRET, TIMESTAMP, BODY);
        Instant dezMinutosDepois = AGORA.plus(Duration.ofMinutes(10));

        assertFalse(WebhookSignature.verify(SECRET, header, BODY, dezMinutosDepois, TOLERANCIA));
    }

    @Test
    void rejeitaTimestampTrocadoPeloAtacante() {
        // Reaproveitar a assinatura antiga com um t novo nao funciona: o t faz parte do que foi assinado
        String assinaturaAntiga = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        long novoT = TIMESTAMP + 600;
        String forjado = "t=" + novoT + ",v1=" + assinaturaAntiga;

        assertFalse(WebhookSignature.verify(SECRET, forjado, BODY, Instant.ofEpochSecond(novoT), TOLERANCIA));
    }

    @Test
    void rejeitaHeaderAusenteOuMalFormado() {
        assertFalse(WebhookSignature.verify(SECRET, null, BODY, AGORA, TOLERANCIA));
        assertFalse(WebhookSignature.verify(SECRET, "lixo", BODY, AGORA, TOLERANCIA));
        assertFalse(WebhookSignature.verify(SECRET, "t=abc,v1=00", BODY, AGORA, TOLERANCIA));
        assertFalse(WebhookSignature.verify(SECRET, "v1=00", BODY, AGORA, TOLERANCIA));
    }

    @Test
    void segredosNovosSaoDiferentesETemPrefixo() {
        String a = WebhookSignature.newSecret();
        String b = WebhookSignature.newSecret();

        assertTrue(a.startsWith("whsec_"));
        assertNotEquals(a, b);
    }
}
