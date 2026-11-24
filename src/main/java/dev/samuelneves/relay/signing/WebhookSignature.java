package dev.samuelneves.relay.signing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Assinatura das entregas, no mesmo formato do Stripe:
 *
 *   X-Relay-Signature: t=1700000000,v1=<hex de HMAC-SHA256(segredo, "1700000000." + corpo)>
 *
 * O timestamp entra no conteudo assinado: alguem que capture uma requisicao valida
 * nao consegue reenvia-la muito tempo depois (ataque de replay), porque o destino
 * rejeita timestamps antigos e nao da para trocar o t sem invalidar a assinatura.
 *
 * O "v1" permite trocar de algoritmo no futuro sem quebrar quem ja verifica.
 */
public final class WebhookSignature {

    public static final String HEADER = "X-Relay-Signature";
    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebhookSignature() {
    }

    /** Um segredo novo: 32 bytes aleatorios (256 bits), com prefixo para ser reconhecivel. */
    public static String newSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Valor do header para este corpo, neste instante. */
    public static String header(String secret, long timestampSeconds, byte[] body) {
        return "t=" + timestampSeconds + ",v1=" + sign(secret, timestampSeconds, body);
    }

    /** HMAC-SHA256 em hexadecimal de "timestamp.corpo". */
    public static String sign(String secret, long timestampSeconds, byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            mac.update((timestampSeconds + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 e obrigatorio em toda JVM; se faltar, o ambiente esta quebrado
            throw new IllegalStateException("HMAC-SHA256 indisponivel", e);
        }
    }

    /**
     * O que o DESTINO faz ao receber: confere a assinatura e a idade do timestamp.
     * Fica aqui como referencia de implementacao e para os testes.
     */
    public static boolean verify(String secret, String header, byte[] body, Instant now, Duration tolerance) {
        if (header == null) {
            return false;
        }
        Long timestamp = null;
        String received = null;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) {
                try {
                    timestamp = Long.parseLong(kv[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (kv[0].equals("v1")) {
                received = kv[1];
            }
        }
        if (timestamp == null || received == null) {
            return false;
        }
        long age = Math.abs(now.getEpochSecond() - timestamp);
        if (age > tolerance.toSeconds()) {
            return false;
        }
        String expected = sign(secret, timestamp, body);
        // Comparacao em tempo constante: um equals comum para no primeiro caractere diferente,
        // e medir esse tempo ajudaria um atacante a descobrir a assinatura aos poucos
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                received.getBytes(StandardCharsets.US_ASCII));
    }
}
