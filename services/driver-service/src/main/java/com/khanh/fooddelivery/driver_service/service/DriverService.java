package com.khanh.fooddelivery.driver_service.service;

import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import java.util.List;
import java.util.UUID;

public interface DriverService {

    DriverProfileResponse register(UUID userId, DriverRegistrationRequest request);

    DriverProfileResponse setStatus(UUID driverId, DriverStatusUpdateRequest request);

    DriverAvailabilityResponse setAvailability(UUID userId, DriverAvailabilityRequest request);

    List<UUID> available();

    boolean active(UUID driverId);

    void reserveOffer(UUID driverId, UUID deliveryId);

    void acceptOffer(UUID driverId, UUID deliveryId);

    void releaseOffer(UUID driverId, UUID deliveryId);

    void releaseDelivery(UUID driverId, UUID deliveryId);
}
