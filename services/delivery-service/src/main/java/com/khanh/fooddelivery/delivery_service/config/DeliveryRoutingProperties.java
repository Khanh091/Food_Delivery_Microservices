package com.khanh.fooddelivery.delivery_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.routing")
public class DeliveryRoutingProperties {
    private String vehicle = "car";

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
}
