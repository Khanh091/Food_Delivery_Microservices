package com.khanh.fooddelivery.restaurant_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({InternalApiProperties.class, SystemRoleSyncProperties.class})
public class SystemRoleSyncConfiguration {}