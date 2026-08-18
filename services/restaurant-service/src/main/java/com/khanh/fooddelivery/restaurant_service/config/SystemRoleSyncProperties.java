package com.khanh.fooddelivery.restaurant_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.system-role-sync")
public class SystemRoleSyncProperties {
    private boolean enabled = true;
    private long fixedDelayMs = 5000;
    private int maxRetries = 10;
    private long initialRetryDelayMs = 5000;
    private long maxRetryDelayMs = 60000;
}