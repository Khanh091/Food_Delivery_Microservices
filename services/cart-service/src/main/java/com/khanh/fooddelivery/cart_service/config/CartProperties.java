package com.khanh.fooddelivery.cart_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cart")
public record CartProperties(Duration ttl, int casMaxRetries) {}
