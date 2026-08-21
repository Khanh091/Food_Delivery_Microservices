package com.khanh.fooddelivery.driver_service.service;

import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverService {

    DriverProfileResponse register(UUID userId, DriverRegistrationRequest request);

    Optional<DriverProfileResponse> profile(UUID userId);

    DriverProfileResponse setStatus(UUID driverId, DriverStatusUpdateRequest request);

    DriverAvailabilityResponse setAvailability(UUID userId, DriverAvailabilityRequest request);

    Optional<DriverAvailabilityResponse> availability(UUID userId);

    List<UUID> available();

    boolean active(UUID driverId);

    void reserveOffer(UUID driverId, UUID deliveryId);

    void acceptOffer(UUID driverId, UUID deliveryId);

    void releaseOffer(UUID driverId, UUID deliveryId);

    void releaseDelivery(UUID driverId, UUID deliveryId);
}
