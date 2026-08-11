package com.khanh.fooddelivery.catalog_service.outbox;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
