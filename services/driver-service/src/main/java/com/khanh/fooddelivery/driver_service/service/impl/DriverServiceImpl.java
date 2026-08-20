package com.khanh.fooddelivery.driver_service.service.impl;

import com.khanh.fooddelivery.driver_service.client.UserSystemRoleClient;
import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.driver_service.entity.DriverAvailability;
import com.khanh.fooddelivery.driver_service.entity.DriverProfile;
import com.khanh.fooddelivery.driver_service.mapper.DriverMapper;
import com.khanh.fooddelivery.driver_service.model.DriverStatus;
import com.khanh.fooddelivery.driver_service.repository.DriverAvailabilityRepository;
import com.khanh.fooddelivery.driver_service.repository.DriverProfileRepository;
import com.khanh.fooddelivery.driver_service.service.DriverService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverAvailabilityRepository availability;
    private final DriverProfileRepository profiles;
    private final UserSystemRoleClient systemRoles;
    private final DriverMapper mapper;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Override
    public DriverProfileResponse register(UUID userId, DriverRegistrationRequest request) {
        if (profiles.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Driver profile already exists");
        }

        DriverProfile profile = new DriverProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setStatus(DriverStatus.PENDING);
        profile.setVehicleType(request.vehicleType().trim());
        profile.setVehiclePlate(request.vehiclePlate().trim().toUpperCase());
        return mapper.toResponse(profiles.save(profile));
    }

    @Override
    public DriverProfileResponse setStatus(UUID driverId, DriverStatusUpdateRequest request) {
        DriverProfile profile = profiles.findByUserId(driverId).orElseThrow();
        if (request.status() == DriverStatus.ACTIVE && profile.getStatus() != DriverStatus.ACTIVE) {
            systemRoles.grantDriverRole(driverId, internalApiKey);
        }
        profile.setStatus(request.status());
        if (request.status() != DriverStatus.ACTIVE) {
            availability.findByUserId(driverId).ifPresent(row -> row.setAvailable(false));
        }
        return mapper.toResponse(profile);
    }

    @Override
    public DriverAvailabilityResponse setAvailability(
            UUID userId,
            DriverAvailabilityRequest request
    ) {
        requireActive(userId);
        DriverAvailability row = availability.findByUserIdForUpdate(userId)
                .orElseGet(() -> newAvailability(userId));
        if (request.available()
                && (row.getActiveDeliveryId() != null || row.getPendingOfferDeliveryId() != null)) {
            throw new IllegalStateException("Driver has an active delivery or pending offer");
        }
        row.setAvailable(request.available());
        return mapper.toResponse(availability.save(row));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> available() {
        return availability
                .findTop10ByAvailableTrueAndActiveDeliveryIdIsNullAndPendingOfferDeliveryIdIsNullOrderByUpdatedAtAsc()
                .stream()
                .filter(row -> profiles.findByUserId(row.getUserId())
                        .map(profile -> profile.getStatus() == DriverStatus.ACTIVE)
                        .orElse(false))
                .map(DriverAvailability::getUserId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean active(UUID driverId) {
        return profiles.findByUserId(driverId)
                .map(profile -> profile.getStatus() == DriverStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    public void reserveOffer(UUID driverId, UUID deliveryId) {
        requireActive(driverId);
        DriverAvailability row = availability.findByUserIdForUpdate(driverId).orElseThrow();
        if (!row.isAvailable()
                || row.getActiveDeliveryId() != null
                || row.getPendingOfferDeliveryId() != null) {
            throw new IllegalStateException("Driver unavailable");
        }
        row.setPendingOfferDeliveryId(deliveryId);
    }

    @Override
    public void acceptOffer(UUID driverId, UUID deliveryId) {
        requireActive(driverId);
        DriverAvailability row = availability.findByUserIdForUpdate(driverId).orElseThrow();
        if (!deliveryId.equals(row.getPendingOfferDeliveryId())
                || !row.isAvailable()
                || row.getActiveDeliveryId() != null) {
            throw new IllegalStateException("Driver offer is no longer active");
        }
        row.setPendingOfferDeliveryId(null);
        row.setActiveDeliveryId(deliveryId);
        row.setAvailable(false);
    }

    @Override
    public void releaseOffer(UUID driverId, UUID deliveryId) {
        availability.findByUserIdForUpdate(driverId)
                .filter(row -> deliveryId.equals(row.getPendingOfferDeliveryId()))
                .ifPresent(row -> row.setPendingOfferDeliveryId(null));
    }

    @Override
    public void releaseDelivery(UUID driverId, UUID deliveryId) {
        availability.findByUserIdForUpdate(driverId)
                .filter(row -> deliveryId.equals(row.getActiveDeliveryId()))
                .ifPresent(row -> {
                    row.setActiveDeliveryId(null);
                    row.setAvailable(active(driverId));
                });
    }

    private DriverAvailability newAvailability(UUID userId) {
        DriverAvailability row = new DriverAvailability();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        return row;
    }

    private void requireActive(UUID userId) {
        if (!active(userId)) {
            throw new IllegalStateException("Driver is not active");
        }
    }
}
