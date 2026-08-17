package com.khanh.fooddelivery.delivery_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "delivery.vietmap")
public class DeliveryVietMapProperties {
    private String serviceKey;
    private String apiBaseUrl = "https://maps.vietmap.vn";
    private int displayType = 5;
}
