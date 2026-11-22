package dev.samuelneves.relay.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuracoes com prefixo relay.delivery, lidas do application.properties.
 * O Spring converte "10s,30s,2m" numa lista de Duration sozinho.
 */
@ConfigurationProperties("relay.delivery")
public record DeliveryProperties(List<Duration> retryDelays) {
}
