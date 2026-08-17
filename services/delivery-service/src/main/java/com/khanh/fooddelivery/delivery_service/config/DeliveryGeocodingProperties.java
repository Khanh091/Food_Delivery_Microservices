package com.khanh.fooddelivery.delivery_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.geocoding")
public class DeliveryGeocodingProperties {
    private String provider = "google";
    private String apiKey = "";
    private String googleBaseUrl = "https://maps.googleapis.com";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getGoogleBaseUrl() { return googleBaseUrl; }
    public void setGoogleBaseUrl(String googleBaseUrl) { this.googleBaseUrl = googleBaseUrl; }
}
