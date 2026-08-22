package com.khanh.fooddelivery.delivery_service.outbox;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeliveryOutboxProperties.class)
public class DeliveryOutboxConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
