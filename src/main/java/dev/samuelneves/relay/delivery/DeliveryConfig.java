package dev.samuelneves.relay.delivery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class DeliveryConfig {

    @Bean
    public RetryPolicy retryPolicy() {
        return RetryPolicy.defaults();
    }
}
