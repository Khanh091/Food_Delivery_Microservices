package com.khanh.fooddelivery.delivery_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.routing")
public class DeliveryRoutingProperties {
    private String provider = "google";
    private String googleBaseUrl = "https://routes.googleapis.com";
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getGoogleBaseUrl() { return googleBaseUrl; }
    public void setGoogleBaseUrl(String googleBaseUrl) { this.googleBaseUrl = googleBaseUrl; }
}
