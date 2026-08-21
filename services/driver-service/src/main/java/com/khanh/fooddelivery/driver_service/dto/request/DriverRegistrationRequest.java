package com.khanh.fooddelivery.driver_service.dto.request;

import com.khanh.fooddelivery.driver_service.model.VehicleType;

public record DriverRegistrationRequest(VehicleType vehicleType, String vehiclePlate) {
}
