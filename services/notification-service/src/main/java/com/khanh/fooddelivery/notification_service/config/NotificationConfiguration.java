package com.khanh.fooddelivery.notification_service.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
