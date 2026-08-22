package com.khanh.fooddelivery.payment_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.internal-api")
public class InternalApiProperties {
    private String key = "";
}
