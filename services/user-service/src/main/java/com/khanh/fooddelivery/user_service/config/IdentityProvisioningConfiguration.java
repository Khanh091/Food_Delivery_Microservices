package com.khanh.fooddelivery.user_service.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({InternalApiProperties.class, KeycloakAdminProperties.class})
public class IdentityProvisioningConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
