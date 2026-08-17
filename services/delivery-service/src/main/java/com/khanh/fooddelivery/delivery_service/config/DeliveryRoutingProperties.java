package com.khanh.fooddelivery.delivery_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.routing")
public class DeliveryRoutingProperties {
    /** VietMap Route v4 vehicle; car preserves the former driving-car behaviour. */
    private String vehicle = "car";

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
}
