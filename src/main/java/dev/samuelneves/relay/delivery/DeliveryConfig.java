package dev.samuelneves.relay.delivery;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DeliveryProperties.class)
public class DeliveryConfig {

    @Bean
    public RetryPolicy retryPolicy(DeliveryProperties properties) {
        return new RetryPolicy(properties.retryDelays());
    }
}
