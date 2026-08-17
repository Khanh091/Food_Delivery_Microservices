package com.khanh.fooddelivery.delivery_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.checkout-location")
public class DeliveryCheckoutLocationProperties {
    private Duration ttl = Duration.ofMinutes(45);
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
}
