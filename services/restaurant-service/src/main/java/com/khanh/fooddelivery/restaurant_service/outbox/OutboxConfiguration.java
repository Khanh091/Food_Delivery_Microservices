package com.khanh.fooddelivery.restaurant_service.outbox;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
}
